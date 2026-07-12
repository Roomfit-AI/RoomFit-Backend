# RoomFit Backend

RoomFit Backend is the Spring Boot backend server for **RoomFit**, an AI-assisted room layout recommendation service.

The backend receives RoomPlan-based room data, builds user preference context, recommends furniture layouts, applies natural language feedback through an LLM-assisted intent parser, and validates the final layout with deterministic placement rules.

> LLMs are used for **intent parsing**, not for directly generating furniture coordinates.

---

## Live Demo

| Item | URL |
|---|---|
| Base URL | `https://roomfit-backend.onrender.com` |
| Swagger UI | `https://roomfit-backend.onrender.com/swagger-ui/index.html` |
| Health Check | `https://roomfit-backend.onrender.com/health` |

> Render Free Tier may sleep when inactive, so the first request can be slow.

---

## Architecture

```text
RoomPlan iOS App
→ RoomFit JSON Upload
→ Spring Boot Backend
→ Agent Context / Product Selection
→ Layout Recommendation
→ LLM Feedback Intent Parsing
→ Rule-based Layout Modification
→ Placement Validation
→ Web Frontend Rendering
```

The backend contains the core AI-assisted layout flow, while the frontend focuses on user interaction and 3D rendering.

---

## Key Features

- **Room upload API**: receives room structure data from the iOS RoomPlan app.
- **Agent context API**: builds user preference context from lifestyle goals, design styles, required items, selected style images, and selected products.
- **Product-aware layout recommendation**: generates furniture layouts using room data, product dimensions, style tags, and clearance rules.
- **LLM feedback intent parsing**: converts natural language feedback into structured layout modification intents.
- **Deterministic layout modification**: applies feedback through backend rules instead of letting the LLM directly generate coordinates.
- **Placement validation**: checks collision, room boundary, door/window clearance, and movement path.
- **Fallback strategy**: falls back to rule-based feedback parsing if the LLM call fails.

---

## AI Feedback Agent

RoomFit supports natural language layout feedback.

Example feedback:

```text
책상을 조금 더 넓게 쓰고 싶어
```

LLM-interpreted intent:

```json
{
  "source": "LLM",
  "rawIntent": "ENLARGE_FURNITURE",
  "targetFurniture": "desk",
  "deskMinWidth": 1.4,
  "constraints": {
    "minWidth": 1.4
  },
  "fallbackUsed": false
}
```

Backend result:

```text
desk width: 1.0m → 1.4m
```

### Design Principle

The LLM does **not** directly generate:

- furniture `x` position
- furniture `z` position
- `rotation`
- final layout coordinates
- validation results

Instead, the LLM converts user feedback into structured intent, and the backend applies deterministic layout rules.

This makes the system easier to validate, debug, and safely integrate into a real service.

---

## Validation Pipeline

Every recommended or modified layout is validated by the backend.

Validation checks include:

- collision check
- room boundary check
- door clearance check
- window clearance check
- movement path check

If feedback is applied but causes a layout issue, the API can still return `success: true` with validation warnings.

```json
{
  "collisionFree": false,
  "boundaryValid": false,
  "warnings": [
    "가구 충돌이 감지되었습니다.",
    "방 범위를 벗어난 가구가 있습니다."
  ]
}
```

This means the layout was updated, but the frontend should display warnings to the user.

---

## Main APIs

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/health` | Server health check |
| `POST` | `/api/rooms/upload` | Upload RoomPlan room data |
| `GET` | `/api/rooms/{roomId}` | Get room data |
| `GET` | `/api/rooms/samples` | Get sample rooms |
| `GET` | `/api/products/mock` | Get mock furniture products |
| `GET` | `/api/styles/images` | Get style image metadata |
| `POST` | `/api/agent/context` | Create user preference context |
| `POST` | `/api/layouts/recommend` | Create layout recommendation |
| `POST` | `/api/layouts/validate` | Validate modified layout |
| `PUT` | `/api/layouts/{layoutId}` | Update layout |
| `POST` | `/api/layouts/feedback` | Apply natural language feedback |
| `POST` | `/api/layouts/{layoutId}/confirm` | Confirm final layout |

Full API details are available in Swagger UI.

---

## Tech Stack

| Area | Stack |
|---|---|
| Language | Java 21 |
| Backend Framework | Spring Boot |
| Build Tool | Gradle |
| API Docs | Swagger / OpenAPI |
| Deployment | Render |
| Container | Docker |
| LLM Integration | OpenAI-compatible Chat Completions API |
| AI Provider Used | Gemini |
| Storage | In-memory repository for MVP |

---

## Local Development

### Requirements

- Java 21
- Gradle Wrapper

### Run

```bash
./gradlew bootRun
```

Local server:

```text
http://localhost:8080
```

Health check:

```bash
curl -i http://localhost:8080/health
```

### Test

```bash
./gradlew test
```

### Build

```bash
./gradlew clean build
```

---

## Environment Variables

LLM feedback parsing is optional.
Without these variables, the backend can still run with rule-based fallback.

| Variable | Description |
|---|---|
| `ROOMFIT_LLM_FEEDBACK_ENABLED` | Enable LLM feedback parser |
| `ROOMFIT_LLM_API_KEY` | LLM provider API key |
| `ROOMFIT_LLM_BASE_URL` | OpenAI-compatible chat completion endpoint |
| `ROOMFIT_LLM_MODEL` | Model ID |
| `ROOMFIT_LLM_TIMEOUT_MS` | LLM timeout in milliseconds |

Example:

```text
ROOMFIT_LLM_FEEDBACK_ENABLED=true
ROOMFIT_LLM_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai/chat/completions
ROOMFIT_LLM_MODEL=gemini-3.5-flash
ROOMFIT_LLM_TIMEOUT_MS=15000
```

> Never commit real API keys to GitHub.

---

## Deployment Notes

The backend is deployed on Render using Docker.

Current MVP limitations:

- In-memory data resets when the server restarts.
- Product data is mock data.
- Layout recommendation logic is rule-based.
- LLM is used only for feedback intent parsing.
- Render Free Tier can introduce cold-start latency.

---

## Smoke Test

After deployment, run the smoke test script to verify the main backend flow.

```bash
./scripts/smoke-test.sh
```

The script checks:

- health check
- product mock API
- style image API
- sample room API
- agent context creation
- layout recommendation
- LLM feedback parsing

Expected final output:

```text
🎉 Smoke test passed
```

---

## Portfolio Highlights

This backend demonstrates:

- REST API design for an AI-assisted service
- RoomPlan JSON ingestion and room data modeling
- Product-aware furniture layout recommendation
- LLM-based natural language feedback interpretation
- Safe AI architecture using deterministic post-processing
- Layout validation pipeline for collision, boundary, clearance, and path checks
- Fallback handling for LLM failure
- Render deployment and frontend integration support

---

## Roadmap

- Persist rooms, contexts, products, and layouts in a database
- Expand supported feedback intents
- Add recommendation reason generation
- Improve automatic collision resolution after feedback
- Add real product catalog integration
- Add user/session management
- Improve Swagger examples and schema descriptions

---

## License

This project is currently developed for the RoomFit AI/SW competition MVP.