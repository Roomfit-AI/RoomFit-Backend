# OpenAPI Documentation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a static OpenAPI 3.0 document for the implemented RoomFit AI MVP backend API.

**Architecture:** No production code changes. Add `docs/openapi/roomfit-api.yaml` and a JUnit test that validates the file contains all implemented paths and key response fields.

**Tech Stack:** OpenAPI 3.0 YAML, Java 21, JUnit 5

## Global Constraints

- Do not add runtime dependencies.
- Do not add fields that are not implemented.
- Preserve `CommonResponse<T>` response structure.
- Keep `recommendedFurniture` as the layout furniture array field name.

---

### Task 1: Static OpenAPI YAML

**Files:**
- Create: `docs/openapi/roomfit-api.yaml`

- [ ] Add all implemented endpoint paths.
- [ ] Add request schemas.
- [ ] Add response schemas.
- [ ] Add representative request examples.

### Task 2: Documentation Test

**Files:**
- Create: `src/test/java/com/roomfit/docs/OpenApiDocumentTest.java`

- [ ] Check that `docs/openapi/roomfit-api.yaml` exists.
- [ ] Check that all implemented paths are present.
- [ ] Check that key fields are present.

### Task 3: Verification and Commit

- [ ] Run `./gradlew test --tests com.roomfit.docs.OpenApiDocumentTest`.
- [ ] Run `./gradlew test`.
- [ ] Stage docs and test files.
- [ ] Commit with `docs: add openapi specification`.
- [ ] Push `develop`.
- [ ] Update PR body.

