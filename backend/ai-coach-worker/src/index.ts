/**
 * TitanBiosync AI Coach — Cloudflare Worker Proxy
 *
 * Endpoints:
 *   POST /coach/chat             → general chat with the AI personal trainer
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
  webSearch?: boolean;
  /** Optional: Italian exercise names from the local DB. If provided, results are filtered/sorted to prefer these. */
  localExerciseNames?: string[];
  /** Optional: structured hints from the local DB to bind generation to real exercises. */
  localExerciseHints?: LocalExerciseHint[];
  /** Optional: recently used templates to reduce duplicates across generations. */
  previousPlans?: PreviousWorkoutPlanSummary[];
  /** Optional: max average similarity percentage allowed (40..50). */
  similarityThreshold?: number;
}

interface PreviousWorkoutPlanSummary {
  title: string;
  exerciseNames: string[];
}

interface OnlineExerciseCandidate {
  name: string;
  category?: string;
  equipment?: string[];
  muscles?: string[];
}

interface LocalExerciseHint {
  nameIt: string;
  equipment?: string | null;
  muscles?: string[];
}

// ── JSON schema for structured workout plan output ─────────────────────────

const WORKOUT_PLAN_ITEM_SCHEMA = {
  type: "object",
  additionalProperties: false,
  properties: {
    title: { type: "string" },
    notes: { type: ["string", "null"] },
    exercises: {
      type: "array",
      items: {
        type: "object",
        additionalProperties: false,
        properties: {
          nameIt: { type: "string" },
          sets: { type: "integer" },
          reps: { type: "integer" },
          restSeconds: { type: ["integer", "null"] },
          notes: { type: ["string", "null"] },
        },
        required: ["nameIt", "sets", "reps", "restSeconds", "notes"],
      },
    },
  },
  required: ["title", "notes", "exercises"],
};

const WORKOUT_PROGRAM_SCHEMA = {
  type: "object",
  additionalProperties: false,
  properties: {
    title: { type: ["string", "null"] },
    notes: { type: ["string", "null"] },
    plans: {
      type: "array",
      minItems: 1,
      items: WORKOUT_PLAN_ITEM_SCHEMA,
    },
  },
  required: ["title", "notes", "plans"],
};

const DEFAULT_ALLOWED_PLAN_SIMILARITY = 0.45;

// ── System prompts ──────────────────────────────────────────────────────────

const SYSTEM_CHAT = `Sei un personal trainer virtuale di nome "Coach TitanBiosync".
Parli SEMPRE in italiano. Dai consigli pratici, motivazionali e scientificamente fondati
su allenamento, nutrizione e recupero. Sei conciso, diretto e incoraggiante.
Quando l'utente ha dati di allenamento disponibili, usali per personalizzare i consigli.`;

const SYSTEM_GENERATE = `Sei un personal trainer esperto. Crea programmi/schede di allenamento in italiano.
RISPONDI SOLO con JSON valido, senza testo aggiuntivo.
Formato obbligatorio:
- title: titolo programma (string o null)
- notes: note programma (string o null)
- plans: array di 1+ giorni/schede
Ogni elemento di plans deve avere:
- title, notes, exercises
Ogni esercizio deve avere: nameIt, sets, reps, restSeconds, notes.
Se l'utente chiede un programma su N giorni, crea N elementi in plans.
Se l'utente chiede una scheda singola, restituisci plans con 1 elemento.`;

// ── Helper: context building ────────────────────────────────────────────────

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

const SPEC_STOPWORDS = new Set([
  "scheda", "allenamento", "allenamenti", "giorni", "giorno", "con", "senza", "per", "della",
  "delle", "degli", "dello", "dell", "dalla", "dalle", "degli", "gli", "con", "una", "uno",
  "intermedio", "principiante", "avanzato", "massa", "ipertrofia", "forza", "dimagrimento", "cardio",
  "voglio", "vorrei", "programma", "routine", "settimana", "settimane", "home", "gym", "palestra"
]);

function extractSearchTerms(userSpec: string, maxTerms = 5): string[] {
  const tokens = userSpec
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, " ")
    .split(/\s+/)
    .map((t) => t.trim())
    .filter((t) => t.length >= 3 && !SPEC_STOPWORDS.has(t));
  return Array.from(new Set(tokens)).slice(0, maxTerms);
}

function stripHtml(input: string): string {
  return input.replace(/<[^>]*>/g, " ").replace(/\s+/g, " ").trim();
}

async function fetchOnlineExerciseCandidates(userSpec: string, maxResults = 12): Promise<OnlineExerciseCandidate[]> {
  const terms = extractSearchTerms(userSpec);
  if (terms.length === 0) return [];

  const dedup = new Map<string, OnlineExerciseCandidate>();

  for (const term of terms) {
    // wger is a public exercise API. We keep this best-effort and fallback-safe.
    const url = `https://wger.de/api/v2/exerciseinfo/?language=4&limit=12&search=${encodeURIComponent(term)}`;
    const resp = await fetch(url, { method: "GET" });
    if (!resp.ok) continue;

    const payload = await resp.json() as {
      results?: Array<{
        name?: string;
        category?: { name?: string };
        equipment?: Array<{ name?: string }>;
        muscles?: Array<{ name?: string }>;
      }>;
    };

    const results = payload.results ?? [];
    for (const item of results) {
      const name = (item.name ?? "").trim();
      if (!name) continue;

      const key = name.toLowerCase();
      if (dedup.has(key)) continue;

      const category = item.category?.name?.trim();
      const equipment = (item.equipment ?? [])
        .map((e) => (e.name ?? "").trim())
        .filter((v) => v.length > 0)
        .slice(0, 3);
      const muscles = (item.muscles ?? [])
        .map((m) => (m.name ?? "").trim())
        .filter((v) => v.length > 0)
        .slice(0, 3);

      dedup.set(key, {
        name: stripHtml(name),
        category: category ? stripHtml(category) : undefined,
        equipment,
        muscles,
      });

      if (dedup.size >= maxResults) break;
    }

    if (dedup.size >= maxResults) break;
  }

  return Array.from(dedup.values()).slice(0, maxResults);
}

function buildWebSearchContext(results: OnlineExerciseCandidate[], localNames?: string[]): string {
  if (results.length === 0) return "";

  // Sort: exercises matching local DB first
  let sorted = results;
  if (localNames && localNames.length > 0) {
    const localNamesLower = new Set(localNames.map((n) => n.toLowerCase()));
    const inLocal: OnlineExerciseCandidate[] = [];
    const notInLocal: OnlineExerciseCandidate[] = [];
    for (const r of results) {
      if (localNamesLower.has(r.name.toLowerCase())) {
        inLocal.push(r);
      } else {
        notInLocal.push(r);
      }
    }
    sorted = [...inLocal, ...notInLocal];
  }

  const lines = sorted.map((r) => {
    const details = [
      r.category ? `categoria: ${r.category}` : null,
      r.equipment && r.equipment.length > 0 ? `attrezzi: ${r.equipment.join(", ")}` : null,
      r.muscles && r.muscles.length > 0 ? `muscoli: ${r.muscles.join(", ")}` : null,
    ].filter(Boolean);
    return details.length > 0 ? `  - ${r.name} (${details.join(" | ")})` : `  - ${r.name}`;
  });
  return `\n\n[Esercizi trovati online]\n${lines.join("\n")}`;
}

function buildLocalExerciseContext(hints: LocalExerciseHint[]): string {
  if (hints.length === 0) return "";
  const lines = hints.map((h) => {
    const details = [
      h.equipment ? `attrezzo: ${h.equipment}` : null,
      h.muscles && h.muscles.length > 0 ? `muscoli: ${h.muscles.slice(0, 4).join(", ")}` : null,
    ].filter(Boolean);
    return details.length > 0 ? `  - ${h.nameIt} (${details.join(" | ")})` : `  - ${h.nameIt}`;
  });
  return `\n\n[Esercizi locali disponibili]\n${lines.join("\n")}`;
}

function buildPreviousPlansContext(previousPlans?: PreviousWorkoutPlanSummary[]): string {
  if (!previousPlans || previousPlans.length === 0) return "";

  const lines = previousPlans
    .slice(0, 6)
    .map((plan, index) => {
      const title = plan.title?.trim() || `Scheda ${index + 1}`;
      const exerciseList = (plan.exerciseNames ?? [])
        .map((name) => name.trim())
        .filter((name) => name.length > 0)
        .slice(0, 14)
        .join(", ");
      return exerciseList.length > 0 ? `  - ${title}: ${exerciseList}` : `  - ${title}`;
    });

  if (lines.length === 0) return "";
  return `\n\n[Schede recenti da non duplicare]\n${lines.join("\n")}`;
}

// ── Similarity helpers ──────────────────────────────────────────────────────

function normalizeExerciseName(name: string): string {
  return name
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9\s]/gi, " ")
    .toLowerCase()
    .replace(/\s+/g, " ")
    .trim();
}

function toExerciseNameSet(names: string[]): Set<string> {
  const set = new Set<string>();
  for (const rawName of names) {
    const normalized = normalizeExerciseName(rawName);
    if (normalized.length > 0) {
      set.add(normalized);
    }
  }
  return set;
}

function jaccardSimilarity(a: Set<string>, b: Set<string>): number {
  if (a.size === 0 || b.size === 0) return 0;
  let intersection = 0;
  for (const item of a) {
    if (b.has(item)) intersection += 1;
  }
  const union = a.size + b.size - intersection;
  if (union <= 0) return 0;
  return intersection / union;
}

function extractExerciseNamesFromGeneratedPlan(plan: unknown): string[] {
  if (!plan || typeof plan !== "object") return [];
  const exercisesUnknown = (plan as { exercises?: unknown }).exercises;
  if (!Array.isArray(exercisesUnknown)) return [];

  return exercisesUnknown
    .map((exercise) => {
      if (!exercise || typeof exercise !== "object") return "";
      const nameIt = (exercise as { nameIt?: unknown }).nameIt;
      return typeof nameIt === "string" ? nameIt : "";
    })
    .map((name) => name.trim())
    .filter((name) => name.length > 0);
}

function computeProgramSimilarity(
  generatedPlans: unknown[],
  previousPlans: PreviousWorkoutPlanSummary[]
): number {
  if (generatedPlans.length === 0 || previousPlans.length === 0) return 0;

  const generatedSets = generatedPlans
    .map((plan) => toExerciseNameSet(extractExerciseNamesFromGeneratedPlan(plan)))
    .filter((set) => set.size > 0);
  const previousSets = previousPlans
    .map((plan) => toExerciseNameSet(plan.exerciseNames ?? []))
    .filter((set) => set.size > 0);

  if (generatedSets.length === 0 || previousSets.length === 0) return 0;

  const bestSimilarities = generatedSets.map((generatedSet) => {
    let best = 0;
    for (const previousSet of previousSets) {
      const similarity = jaccardSimilarity(generatedSet, previousSet);
      if (similarity > best) best = similarity;
    }
    return best;
  });

  const sum = bestSimilarities.reduce((acc, value) => acc + value, 0);
  return sum / bestSimilarities.length;
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

const PRIMARY_MODEL = "gpt-4.1-mini";
const FALLBACK_MODEL = "gpt-4o-mini";

function extractOpenAiErrorDetail(raw: string): string {
  if (!raw) return "risposta vuota dal provider AI";
  try {
    const parsed = JSON.parse(raw) as { error?: { message?: string } };
    return parsed?.error?.message?.trim() || raw;
  } catch {
    return raw;
  }
}

function shouldRetryWithFallbackModel(status: number, rawErrorBody: string): boolean {
  if (!(status === 400 || status === 404)) return false;
  const detail = extractOpenAiErrorDetail(rawErrorBody).toLowerCase();
  return detail.includes("model") && (detail.includes("not found") || detail.includes("does not exist"));
}

async function callOpenAIWithModelFallback(
  apiKey: string,
  payload: Record<string, unknown>
): Promise<Response> {
  const first = await callOpenAI(apiKey, { ...payload, model: PRIMARY_MODEL });
  if (first.ok) return first;

  const firstRaw = await first.text();
  if (!shouldRetryWithFallbackModel(first.status, firstRaw)) {
    return new Response(firstRaw, {
      status: first.status,
      headers: { "Content-Type": "application/json" },
    });
  }

  return callOpenAI(apiKey, { ...payload, model: FALLBACK_MODEL });
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

  if (!env.OPENAI_API_KEY) {
    return jsonError("OPENAI_API_KEY non configurata nel Worker", 500);
  }

  let openAiResp: Response;
  try {
    openAiResp = await callOpenAIWithModelFallback(env.OPENAI_API_KEY, {
      messages: openAiMessages,
      max_tokens: 800,
      temperature: 0.7,
    });
  } catch (error) {
    console.error("OpenAI fetch error (chat):", error);
    return jsonError("Errore di connessione verso il servizio AI", 502);
  }

  if (!openAiResp.ok) {
    const errText = await openAiResp.text().catch(() => "");
    const detail = extractOpenAiErrorDetail(errText);
    console.error("OpenAI error (chat):", openAiResp.status, errText);
    return jsonError(`Errore dal servizio AI (OpenAI ${openAiResp.status}): ${detail}`, 502);
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

  const { userSpec, history, webSearch, localExerciseNames, localExerciseHints, previousPlans } = body;
  if (!userSpec || typeof userSpec !== "string") {
    return jsonError("Campo 'userSpec' mancante", 400);
  }

  const thresholdPercent = Number.isFinite(body.similarityThreshold)
    ? Number(body.similarityThreshold)
    : DEFAULT_ALLOWED_PLAN_SIMILARITY * 100;
  const safeThresholdPercent = Math.min(50, Math.max(40, thresholdPercent));
  const allowedPlanSimilarity = safeThresholdPercent / 100;

  const historyCtx = buildHistoryContext(history);
  const localCtx = localExerciseHints && localExerciseHints.length > 0
    ? buildLocalExerciseContext(localExerciseHints.slice(0, 18))
    : "";
  const previousPlansCtx = previousPlans && previousPlans.length > 0
    ? buildPreviousPlansContext(previousPlans)
    : "";

  let webCtx = "";
  let webResultsCount = 0;
  if (webSearch !== false) {
    try {
      const webCandidates = await fetchOnlineExerciseCandidates(userSpec);
      webResultsCount = webCandidates.length;
      webCtx = buildWebSearchContext(webCandidates, localExerciseNames);
    } catch (error) {
      console.warn("Web search enrichment skipped:", error);
    }
  }

  const webInstruction = webCtx
    ? "\nUsa preferibilmente gli esercizi trovati online se coerenti con la richiesta; se non sono adatti, scegli esercizi equivalenti."
    : "";
  const localInstruction = localCtx
    ? "\nVincolo: scegli prima tra gli esercizi locali disponibili; usa esercizi online solo se non trovi un equivalente locale adeguato."
    : "";
  const variationInstruction = previousPlansCtx
    ? `\nVincolo anti-duplicato: evita di replicare le schede recenti. La similarita media con le schede recenti deve essere <= ${safeThresholdPercent.toFixed(0)}%.`
    : "";

  const basePrompt = `Crea una scheda di allenamento con le seguenti specifiche: ${userSpec}${historyCtx}${localCtx}${webCtx}${previousPlansCtx}${localInstruction}${webInstruction}${variationInstruction}`;

  if (!env.OPENAI_API_KEY) {
    return jsonError("OPENAI_API_KEY non configurata nel Worker", 500);
  }

  const requestProgram = async (prompt: string): Promise<{
    program: { title: string | null; notes: string | null; plans: object[] };
    tokensUsed: number;
  }> => {
    let openAiResp: Response;
    try {
      openAiResp = await callOpenAIWithModelFallback(env.OPENAI_API_KEY, {
        messages: [
          { role: "system", content: SYSTEM_GENERATE },
          { role: "user", content: prompt },
        ],
        response_format: {
          type: "json_schema",
          json_schema: {
            name: "workout_plan",
            strict: true,
            schema: WORKOUT_PROGRAM_SCHEMA,
          },
        },
        max_tokens: 1200,
        temperature: 0.5,
      });
    } catch (error) {
      console.error("OpenAI fetch error (generate-workout):", error);
      throw new Error("OPENAI_CONNECTION_ERROR");
    }

    if (!openAiResp.ok) {
      const errText = await openAiResp.text().catch(() => "");
      const detail = extractOpenAiErrorDetail(errText);
      console.error("OpenAI error (generate-workout):", openAiResp.status, errText);
      throw new Error(`OPENAI_HTTP_${openAiResp.status}:${detail}`);
    }

    const data = await openAiResp.json() as {
      choices: { message: { content: string } }[];
      usage?: { total_tokens: number };
    };

    const planJson = data.choices?.[0]?.message?.content ?? "{}";
    const tokensUsed = data.usage?.total_tokens ?? 0;

    let program: { title: string | null; notes: string | null; plans: object[] };
    try {
      program = JSON.parse(planJson) as { title: string | null; notes: string | null; plans: object[] };
    } catch {
      throw new Error("INVALID_JSON_RESPONSE");
    }

    return { program, tokensUsed };
  };

  let program: { title: string | null; notes: string | null; plans: object[] };
  let tokensUsed = 0;

  try {
    const firstAttempt = await requestProgram(basePrompt);
    program = firstAttempt.program;
    tokensUsed += firstAttempt.tokensUsed;

    const firstPlans = Array.isArray(program.plans) ? program.plans : [];
    const canEvaluateSimilarity =
      Array.isArray(previousPlans) &&
      previousPlans.length > 0 &&
      firstPlans.length > 0;

    if (canEvaluateSimilarity) {
      const similarity = computeProgramSimilarity(firstPlans, previousPlans);
      if (similarity > allowedPlanSimilarity) {
        const retryPrompt = `${basePrompt}\n\nLa proposta precedente era troppo simile alle schede recenti (similarita media ${(similarity * 100).toFixed(0)}%). Rigenera con maggiore novita: cambia almeno il 50% degli esercizi per ogni giorno mantenendo obiettivo, livello e sicurezza.`;
        const secondAttempt = await requestProgram(retryPrompt);
        program = secondAttempt.program;
        tokensUsed += secondAttempt.tokensUsed;
      }
    }
  } catch (error) {
    const message = error instanceof Error ? error.message : "UNKNOWN_WORKOUT_ERROR";
    if (message === "OPENAI_CONNECTION_ERROR") {
      return jsonError("Errore di connessione verso il servizio AI", 502);
    }
    if (message === "INVALID_JSON_RESPONSE") {
      return jsonError("Il modello non ha restituito JSON valido", 502);
    }
    if (message.startsWith("OPENAI_HTTP_")) {
      const parts = message.replace("OPENAI_HTTP_", "").split(":");
      const status = Number(parts[0]) || 500;
      const detail = parts.slice(1).join(":") || "errore sconosciuto";
      return jsonError(`Errore dal servizio AI (OpenAI ${status}): ${detail}`, 502);
    }

    console.error("Unexpected generate-workout error:", error);
    return jsonError("Errore interno durante la generazione della scheda", 500);
  }

  const plans = Array.isArray(program.plans) ? program.plans : [];
  if (plans.length === 0) {
    return jsonError("Il modello non ha restituito una scheda valida", 502);
  }

  return jsonOk({
    message: plans.length > 1
      ? `Programma generato con successo (${plans.length} giorni)! Puoi rivederlo e salvarlo nella cartella 'Schede da AI'.`
      : "Scheda generata con successo! Puoi rivederla e salvarla nella cartella 'Schede da AI'.",
    plan: plans[0],
    plans,
    tokensUsed,
    webSearchUsed: webSearch !== false,
    webResultsCount,
    localHintsCount: localExerciseHints?.length ?? 0,
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
    try {
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
    } catch (error) {
      const message = error instanceof Error ? error.message : "errore sconosciuto";
      console.error("Worker unhandled error:", error);
      return jsonError(`Errore interno Worker: ${message}`, 500);
    }
  },
};
