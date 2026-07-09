# Layout Confirm API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `POST /api/layouts/{layoutId}/confirm`의 성공/실패 응답 구조를 명세 기준 테스트로 고정한다.

**Architecture:** 기존 `LayoutController`, `LayoutService`, `ConfirmResponse`, `LayoutRepository`를 유지한다. 구현이 명세를 만족하면 production 코드는 변경하지 않는다.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, MockMvc, in-memory Map Repository

## Global Constraints

- API 응답 body는 `success`, `data`, `error`만 포함한다.
- 명세에 없는 필드는 추가하지 않는다.
- 이미 확정된 layout은 다시 확정할 수 없다.

---

### Task 1: Confirm API 테스트

**Files:**
- Create: `src/test/java/com/roomfit/placement/LayoutConfirmControllerTest.java`

**Interfaces:**
- Consumes: `POST /api/agent/context`, `POST /api/layouts/recommend`, `POST /api/layouts/{layoutId}/confirm`
- Produces: confirm API 성공/실패 케이스 회귀 테스트

- [ ] 정상 확정 테스트를 작성한다.
- [ ] 없는 layoutId 실패 테스트를 작성한다.
- [ ] 이미 확정된 layout 중복 확정 실패 테스트를 작성한다.
- [ ] `./gradlew test --tests com.roomfit.placement.LayoutConfirmControllerTest`를 실행한다.

### Task 2: 필요 시 최소 구현 보강

**Files:**
- Modify only if target test fails for a real contract mismatch.

- [ ] 테스트 실패 원인을 확인한다.
- [ ] 기존 구현이 명세와 다를 때만 production 코드를 수정한다.
- [ ] 타깃 테스트를 다시 실행한다.

### Task 3: 검증과 커밋

**Files:**
- All changed files from Tasks 1-2

- [ ] `./gradlew test`를 실행한다.
- [ ] `AGENTS.md`, `gradle.properties`를 제외하고 stage한다.
- [ ] `test: cover layout confirm api`로 커밋한다.
- [ ] `develop`에 push한다.
- [ ] PR 설명에 confirm API 테스트를 추가한다.
