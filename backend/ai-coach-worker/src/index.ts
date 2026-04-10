/**
 * TitanBiosync AI Coach — Cloudflare Worker Proxy
 *
 * Endpoints:
 *   POST /coach/chat            → general chat with the AI personal trainer
 *   POST /coach/generate-workout → generate a structured workout plan (scheda)
 *
 * Environment variables (set via `wrangler secret put`):
 *   OPENAI_API_KEY   – OpenAI secret key (never exposed to the client)
 *   APP_TOKEN        – shared token sent by the Android app in X-App-Token header
 *
 * The worker authenticates requests by checking X-App-Token against APP_TOKEN.
 */

export interface Env {
  OPENAI_API_KEY: string;
  APP_TOKEN: string;
}

// ── Types matching the Android DTOs ────────────────────────────────────────

interface ChatMessage {
  role: "user" | "assistant" | "system";
  content: string;
}

interface WorkoutHistorySummary {
  recentSessions?: RecentSession[];
  topPrs?: ExercisePr[];
  weeklyFrequency?: number;
}

interface RecentSession {
  templateName: string;
  date: string;
  durationMin: number;
  totalVolumeKg: number;
}

interface ExercisePr {
  exerciseName: string;
  maxWeightKg: number;
  maxE1rm: number;
}

interface ChatRequest {
  messages: ChatMessage[];
  history?: WorkoutHistorySummary;
}

interface GenerateWorkoutRequest {
  userSpec: string;
  history?: WorkoutHistorySummary;
}

// ── JSON schema for structured workout plan output ─────────────────────────

const WORKOUT_PLAN_SCHEMA = {
  type: "object",
  properties: {
    title: { type: "string" },
    notes: { type: "string" },
    exercises: {
      type: "array",
      items: {
        type: "object",
        properties: {
          nameIt: { type: "string" },
          sets: { type: "integer" },
          reps: { type: "integer" },
          restSeconds: { type: "integer" },
          notes: { type: "string" },
        },
        required: ["nameIt", "sets", "reps"],
      },
    },
  },
  required: ["title", "exercises"],
};

// ── System prompts ──────────────────────────────────────────────────────────

const SYSTEM_CHAT = `Sei un personal trainer virtuale di nome "Coach TitanBiosync". 
Parli SEMPRE in italiano. Dai consigli pratici, motivazionali e scientificamente fondati 
su allenamento, nutrizione e recupero. Sei conciso, diretto e incoraggiante.
Quando l'utente ha dati di allenamento disponibili, usali per personalizzare i consigli.`;

const SYSTEM_GENERATE = `Sei un personal trainer esperto. Crea schede di allenamento in italiano.
RISPONDI SOLO con il JSON della scheda, senza testo aggiuntivo.
Usa nomi di esercizi in italiano (es. "Panca Piana", "Squat", "Stacchi da Terra").
Ogni esercizio deve avere: nameIt (nome italiano), sets (serie), reps (ripetizioni), restSeconds (recupero in secondi).`;

// ── Helper: build summary prompt addition ──────────────────────────────────

function buildHistoryContext(history?: WorkoutHistorySummary): string {
  if (!history) return "";

  const parts: string[] = [];

  if (history.weeklyFrequency != null) {
    parts.push(`Frequenza settimanale media: ${history.weeklyFrequency} sessioni/settimana.`);
  }

  if (history.recentSessions && history.recentSessions.length > 0) {
    const sessions = history.recentSessions
      .map(
        (s) =>
          `  - ${s.date}: ${s.templateName} (${s.durationMin} min, ${s.totalVolumeKg.toFixed(0)} kg volume)`
      )
      .join("\n");
    parts.push(`Sessioni recenti:\n${sessions}`);
  }

  if (history.topPrs && history.topPrs.length > 0) {
    const prs = history.topPrs
      .map((p) => `  - ${p.exerciseName}: ${p.maxWeightKg} kg (e1RM: ${p.maxE1rm.toFixed(1)} kg)`)
      .join("\n");
    parts.push(`Record personali:\n${prs}`);
  }

  if (parts.length === 0) return "";
  return `\n\n[Dati allenamento utente]\n${parts.join("\n")}`;
}

// ── OpenAI helpers ─────────────────────────────────────────────────────────

async function callOpenAI(
  apiKey: string,
  payload: object
): Promise<Response> {
  return fetch("https://api.openai.com/v1/chat/completions", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${apiKey}`,
    },
    body: JSON.stringify(payload),
  });
}

// ── Request handlers ───────────────────────────────────────────────────────

async function handleChat(req: Request, env: Env): Promise<Response> {
  let body: ChatRequest;
  try {
    body = await req.json() as ChatRequest;
  } catch {
    return jsonError("Corpo della richiesta non valido", 400);
  }

  const { messages, history } = body;
  if (!messages || !Array.isArray(messages)) {
    return jsonError("Campo 'messages' mancante o non valido", 400);
  }

  const systemContent = SYSTEM_CHAT + buildHistoryContext(history);

  const openAiMessages: ChatMessage[] = [
    { role: "system", content: systemContent },
    ...messages,
  ];

  const openAiResp = await callOpenAI(env.OPENAI_API_KEY, {
    model: "gpt-4o-mini",
    messages: openAiMessages,
    max_tokens: 800,
    temperature: 0.7,
  });

  if (!openAiResp.ok) {
    const errText = await openAiResp.text();
    console.error("OpenAI error:", openAiResp.status, errText);
    return jsonError("Errore dal servizio AI", 502);
  }

  const data = await openAiResp.json() as {
    choices: { message: { content: string } }[];
    usage?: { total_tokens: number };
  };

  const assistantMessage = data.choices?.[0]?.message?.content ?? "";
  const tokensUsed = data.usage?.total_tokens ?? 0;

  return jsonOk({ message: assistantMessage, tokensUsed });
}

async function handleGenerateWorkout(req: Request, env: Env): Promise<Response> {
  let body: GenerateWorkoutRequest;
  try {
    body = await req.json() as GenerateWorkoutRequest;
  } catch {
    return jsonError("Corpo della richiesta non valido", 400);
  }

  const { userSpec, history } = body;
  if (!userSpec || typeof userSpec !== "string") {
    return jsonError("Campo 'userSpec' mancante", 400);
  }

  const historyCtx = buildHistoryContext(history);
  const userPrompt = `Crea una scheda di allenamento con le seguenti specifiche: ${userSpec}${historyCtx}`;

  const openAiResp = await callOpenAI(env.OPENAI_API_KEY, {
    model: "gpt-4o-mini",
    messages: [
      { role: "system", content: SYSTEM_GENERATE },
      { role: "user", content: userPrompt },
    ],
    response_format: {
      type: "json_schema",
      json_schema: {
        name: "workout_plan",
        strict: true,
        schema: WORKOUT_PLAN_SCHEMA,
      },
    },
    max_tokens: 1200,
    temperature: 0.5,
  });

  if (!openAiResp.ok) {
    const errText = await openAiResp.text();
    console.error("OpenAI error:", openAiResp.status, errText);
    return jsonError("Errore dal servizio AI", 502);
  }

  const data = await openAiResp.json() as {
    choices: { message: { content: string } }[];
    usage?: { total_tokens: number };
  };

  const planJson = data.choices?.[0]?.message?.content ?? "{}";
  const tokensUsed = data.usage?.total_tokens ?? 0;

  let plan: object;
  try {
    plan = JSON.parse(planJson);
  } catch {
    return jsonError("Il modello non ha restituito JSON valido", 502);
  }

  return jsonOk({
    message: "Scheda generata con successo! Puoi rivederla e salvarla nella cartella 'Schede da AI'.",
    plan,
    tokensUsed,
  });
}

// ── Response helpers ───────────────────────────────────────────────────────

function jsonOk(body: object): Response {
  return new Response(JSON.stringify(body), {
    status: 200,
    headers: corsHeaders({ "Content-Type": "application/json" }),
  });
}

function jsonError(message: string, status: number): Response {
  return new Response(JSON.stringify({ error: message }), {
    status,
    headers: corsHeaders({ "Content-Type": "application/json" }),
  });
}

function corsHeaders(extra: Record<string, string> = {}): Record<string, string> {
  return {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type, X-App-Token",
    ...extra,
  };
}

// ── Main fetch handler ─────────────────────────────────────────────────────

export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    // Handle CORS preflight
    if (request.method === "OPTIONS") {
      return new Response(null, { status: 204, headers: corsHeaders() });
    }

    if (request.method !== "POST") {
      return jsonError("Metodo non supportato", 405);
    }

    // Authenticate request
    const token = request.headers.get("X-App-Token") ?? "";
    if (!env.APP_TOKEN || token !== env.APP_TOKEN) {
      return jsonError("Non autorizzato", 401);
    }

    const url = new URL(request.url);
    const path = url.pathname;

    if (path === "/coach/chat") {
      return handleChat(request, env);
    } else if (path === "/coach/generate-workout") {
      return handleGenerateWorkout(request, env);
    } else {
      return jsonError("Endpoint non trovato", 404);
    }
  },
};
