# Layout Validate API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `POST /api/layouts/validate`가 전체 furniture 배열 기준으로 배치 검증과 명세상 예외 응답을 반환하게 한다.

**Architecture:** 기존 `LayoutController`, `LayoutService`, `ValidationService`를 유지한다. `LayoutService`는 layout 조회와 furniture 배열 일치/좌표/회전 검증을 담당하고, `ValidationService`는 순수 배치 검증 결과를 계산한다.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, MockMvc, in-memory Map Repository

## Global Constraints

- API 응답 body는 `success`, `data`, `error`만 포함한다.
- `POST /api/layouts/validate`는 현재 화면의 전체 furniture 배열을 받는다.
- position은 중심 좌표 기준이다.
- rotation은 degree 기준이다.
- 좌표 단위는 meter이다.
- 검증 항목은 collision, boundary, door_clearance, window_clearance, path이다.
- 이번 단계에서 `PUT /api/layouts/{layoutId}` 저장 로직은 변경하지 않는다.

---

### Task 1: Validate API 실패 테스트

**Files:**
- Create: `src/test/java/com/roomfit/placement/LayoutValidateControllerTest.java`

**Interfaces:**
- Consumes: `POST /api/agent/context`, `POST /api/layouts/recommend`, `POST /api/layouts/validate`
- Produces: validate API 성공/실패 케이스 회귀 테스트

- [ ] `LayoutValidateControllerTest`를 작성한다.
- [ ] 정상 요청에서 `validationResult` 필드와 `validationItems` 5개를 검증한다.
- [ ] 충돌 요청에서 `collisionFree=false`를 검증한다.
- [ ] unknown furniture id, 누락 id, 범위 밖 좌표, 잘못된 rotation의 에러 코드를 검증한다.
- [ ] `./gradlew test --tests com.roomfit.placement.LayoutValidateControllerTest`를 실행해 RED를 확인한다.

### Task 2: ErrorCode와 배열 검증

**Files:**
- Modify: `src/main/java/com/roomfit/common/ErrorCode.java`
- Modify: `src/main/java/com/roomfit/placement/LayoutService.java`

**Interfaces:**
- Consumes: `ValidateRequest.getFurniture()`
- Produces: `applyPositionOverrides(List<Furniture>, List<FurniturePositionDto>, Room)` 내부 검증

- [ ] `FURNITURE_NOT_FOUND`, `FURNITURE_ARRAY_MISMATCH`, `INVALID_ROTATION`을 추가한다.
- [ ] 요청 id 집합과 기존 layout id 집합을 비교한다.
- [ ] layout에 없는 id는 `FURNITURE_NOT_FOUND`로 처리한다.
- [ ] 누락 id는 `FURNITURE_ARRAY_MISMATCH`로 처리한다.
- [ ] 좌표와 rotation을 검증한다.
- [ ] RED 테스트를 다시 실행해 예외 케이스를 통과시킨다.

### Task 3: ValidationService 실제 검증

**Files:**
- Modify: `src/main/java/com/roomfit/placement/ValidationService.java`

**Interfaces:**
- Consumes: `Room`, `List<Furniture>`
- Produces: `ValidationResult`

- [ ] furniture AABB helper를 추가한다.
- [ ] collision, boundary, door clearance, window clearance, path 검증을 구현한다.
- [ ] `validationItems`의 `passed` 값을 실제 boolean과 동기화한다.
- [ ] `warnings`에 실패 항목 메시지를 추가한다.
- [ ] 타깃 테스트와 전체 테스트를 실행한다.

### Task 4: 커밋과 PR 업데이트

**Files:**
- All changed files from Tasks 1-3

- [ ] `./gradlew test`를 실행한다.
- [ ] `AGENTS.md`, `gradle.properties`를 제외하고 stage한다.
- [ ] `feat: add layout validate api`로 커밋한다.
- [ ] `develop`에 push한다.
- [ ] PR 설명에 validate API와 테스트 항목을 추가한다.
