# Layout Feedback API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `POST /api/layouts/feedback`가 대표 자연어 피드백 3개를 해석하고 재추천 응답을 반환하게 한다.

**Architecture:** `LayoutController`에 feedback endpoint를 추가하고, `LayoutService`에서 layout 조회, intent 해석, furniture 재추천, validation 계산, 새 layout 저장을 담당한다. 응답은 별도 `FeedbackResponse` DTO로 분리해 recommend/update 응답 구조를 건드리지 않는다.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, MockMvc, in-memory Map Repository

## Global Constraints

- API 응답 body는 `success`, `data`, `error`만 포함한다.
- 자연어 피드백은 대표 문장 3개만 처리한다.
- 지원하지 않는 문장은 `UNSUPPORTED_FEEDBACK_INTENT / 400`으로 처리한다.
- feedback 응답은 `layoutId`, `status`, `recommendedFurniture`, `scoreSummary`, `validationResult`, `interpretedIntent`를 포함한다.
- 추천 응답의 가구 배열 필드명은 반드시 `recommendedFurniture`로 유지한다.

---

### Task 1: Feedback API 실패 테스트

**Files:**
- Create: `src/test/java/com/roomfit/placement/LayoutFeedbackControllerTest.java`

**Interfaces:**
- Consumes: `POST /api/agent/context`, `POST /api/layouts/recommend`, `POST /api/layouts/feedback`
- Produces: feedback API 성공/실패 케이스 회귀 테스트

- [ ] `책상 더 크게` 성공 테스트를 작성한다.
- [ ] `수납 늘려줘` 성공 테스트를 작성한다.
- [ ] `방이 넓어 보이게` 성공 테스트를 작성한다.
- [ ] unsupported feedback 테스트를 작성한다.
- [ ] unknown layout 테스트를 작성한다.
- [ ] `./gradlew test --tests com.roomfit.placement.LayoutFeedbackControllerTest`로 RED를 확인한다.

### Task 2: DTO와 ErrorCode

**Files:**
- Create: `src/main/java/com/roomfit/placement/dto/FeedbackRequest.java`
- Create: `src/main/java/com/roomfit/placement/dto/FeedbackResponse.java`
- Modify: `src/main/java/com/roomfit/common/ErrorCode.java`

**Interfaces:**
- Produces: `FeedbackRequest.getLayoutId()`, `FeedbackRequest.getFeedback()`, `FeedbackResponse.of(...)`

- [ ] `FeedbackRequest`에 `layoutId`, `feedback` 필드를 추가한다.
- [ ] `FeedbackResponse`에 명세 응답 필드와 getter를 추가한다.
- [ ] `UNSUPPORTED_FEEDBACK_INTENT`를 `ErrorCode`에 추가한다.

### Task 3: Service와 Controller 구현

**Files:**
- Modify: `src/main/java/com/roomfit/placement/LayoutController.java`
- Modify: `src/main/java/com/roomfit/placement/LayoutService.java`

**Interfaces:**
- Consumes: `FeedbackRequest`
- Produces: `LayoutService.feedback(FeedbackRequest): FeedbackResponse`

- [ ] `LayoutController`에 `@PostMapping("/feedback")`를 추가한다.
- [ ] `LayoutService.feedback()`에서 layout과 room을 조회한다.
- [ ] feedback 문장을 3개 intent로 해석한다.
- [ ] intent에 따라 furniture 리스트를 복사/수정한다.
- [ ] 새 `Layout`을 저장하고 `ValidationService.validate()`를 호출한다.
- [ ] `FeedbackResponse`를 반환한다.

### Task 4: 검증과 커밋

**Files:**
- All changed files from Tasks 1-3

- [ ] `./gradlew test --tests com.roomfit.placement.LayoutFeedbackControllerTest`를 실행한다.
- [ ] `./gradlew test`를 실행한다.
- [ ] `AGENTS.md`, `gradle.properties`를 제외하고 stage한다.
- [ ] `feat: add layout feedback api`로 커밋한다.
- [ ] `develop`에 push한다.
- [ ] PR 설명에 feedback API와 테스트 항목을 추가한다.
