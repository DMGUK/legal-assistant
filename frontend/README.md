# Legal Assistant — Frontend

React + Vite frontend for the Legal Assistant RAG application.

## Prerequisites

- Node.js 18+
- The Spring Boot backend running on `http://localhost:8080`

## Setup & Run

```bash
cd frontend
npm install
npm run dev
```

Then open http://localhost:5173 in your browser.

The Vite dev server proxies all `/api/*` requests to `http://localhost:8080`, so no CORS configuration is needed during development.

## Build for production

```bash
npm run build
```

Outputs static files to `frontend/dist/`.

## Usage

1. **Upload a PDF** — drag and drop or click the upload area on the left, then click "Upload & Process".
2. **Ask questions** — type your question and press Enter (or click Send).

## API endpoints used

| Action | Endpoint |
|--------|----------|
| Upload PDF | `POST /api/documents` (multipart `file`) |
| Ask question | `POST /api/documents/{id}/ask` with `{ "question": "..." }` |

## Environment variables (backend)

| Variable | Purpose |
|----------|---------|
| `ANTHROPIC_API_KEY` | Required — Claude API key |
| `OPENAI_API_KEY` | Optional — fallback embedding provider |
