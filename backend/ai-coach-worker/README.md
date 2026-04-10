# TitanBiosync AI Coach — Cloudflare Worker

Backend proxy that securely routes AI requests from the TitanBiosync Android app to OpenAI without exposing the API key in the client.

## Endpoints

| Method | Path                      | Description                                  |
|--------|---------------------------|----------------------------------------------|
| POST   | `/coach/chat`             | General chat with the Italian AI coach       |
| POST   | `/coach/generate-workout` | Generate a structured workout plan (scheda)  |

## Authentication

Every request must include the header:
```
X-App-Token: <APP_TOKEN>
```
The `APP_TOKEN` is a shared secret stored as a Cloudflare Worker secret (never committed to the repo).

## Setup & Deployment

### Prerequisites
- Node.js ≥ 18
- Cloudflare account + Wrangler CLI

### 1. Install dependencies
```bash
cd backend/ai-coach-worker
npm install
```

### 2. Set secrets
```bash
wrangler secret put OPENAI_API_KEY   # paste your OpenAI API key
wrangler secret put APP_TOKEN        # paste a random secret token (e.g. uuid4)
```

### 3. Deploy
```bash
npm run deploy
```

After deploy, Wrangler prints the Worker URL, e.g.:
```
https://titanbiosync-ai-coach.<your-subdomain>.workers.dev
```

### 4. Configure the Android app
In `app/build.gradle.kts`, update `buildConfigField`:
```kotlin
buildConfigField("String", "COACH_API_URL", "\"https://titanbiosync-ai-coach.<your-subdomain>.workers.dev\"")
buildConfigField("String", "COACH_APP_TOKEN", "\"<your-APP_TOKEN>\"")
```

> **Security note**: `COACH_API_URL` is not secret, but `COACH_APP_TOKEN` should ideally be delivered via a secure server-side provisioning step rather than baked into the APK. For MVP, baking it into `buildConfigField` behind a release-only config is acceptable.

## Request / Response Schemas

### POST /coach/chat
```json
{
  "messages": [
    { "role": "user", "content": "Come posso migliorare il mio squat?" }
  ],
  "history": {
    "recentSessions": [
      { "templateName": "Gambe", "date": "2024-01-10", "durationMin": 60, "totalVolumeKg": 4500 }
    ],
    "topPrs": [
      { "exerciseName": "Squat", "maxWeightKg": 100, "maxE1rm": 115 }
    ],
    "weeklyFrequency": 3
  }
}
```
Response:
```json
{ "message": "Per migliorare lo squat ti consiglio...", "tokensUsed": 123 }
```

### POST /coach/generate-workout
```json
{
  "userSpec": "scheda push 4 giorni, ipertrofia, intermedio",
  "history": { ... }
}
```
Response:
```json
{
  "message": "Scheda generata con successo! Puoi rivederla e salvarla nella cartella 'Schede da AI'.",
  "plan": {
    "title": "Push Day A",
    "notes": "Riposo 60-90 secondi tra le serie",
    "exercises": [
      { "nameIt": "Panca Piana", "sets": 4, "reps": 8, "restSeconds": 90 },
      { "nameIt": "Shoulder Press", "sets": 3, "reps": 10, "restSeconds": 75 }
    ]
  },
  "tokensUsed": 456
}
```

## Local Development
```bash
npm run dev
# Worker runs at http://localhost:8787
```
