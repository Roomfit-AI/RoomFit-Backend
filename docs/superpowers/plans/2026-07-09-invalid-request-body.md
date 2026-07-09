# Invalid Request Body Handling Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Spring 기본 400 에러를 `CommonResponse`의 `INVALID_REQUEST_BODY` 응답으로 통일한다.

**Architecture:** 기존 `ErrorCode`, `GlobalExceptionHandler`, `CommonResponse`를 사용한다. 개별 Controller/Service는 변경하지 않는다.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, MockMvc

## Global Constraints

- API 응답 body는 `success`, `data`, `error`만 포함한다.
- 실패 응답도 `CommonResponse<T>` 구조를 따른다.
- 인증/인가는 MVP 범위에서 제외한다.
- 명세에 없는 에러 코드는 추가하지 않는다.

---

### Task 1: Invalid Request 실패 테스트

**Files:**
- Create: `src/test/java/com/roomfit/common/GlobalExceptionHandlerTest.java`

**Interfaces:**
- Consumes: `POST /api/agent/context`, `GET /api/rooms/{roomId}`
- Produces: invalid request body/path variable 공통 예외 테스트

- [ ] malformed JSON body 테스트를 작성한다.
- [ ] 잘못된 path variable 타입 테스트를 작성한다.
- [ ] `./gradlew test --tests com.roomfit.common.GlobalExceptionHandlerTest`로 RED를 확인한다.

### Task 2: ErrorCode와 Handler 구현

**Files:**
- Modify: `src/main/java/com/roomfit/common/ErrorCode.java`
- Modify: `src/main/java/com/roomfit/common/GlobalExceptionHandler.java`

**Interfaces:**
- Produces: `ErrorCode.INVALID_REQUEST_BODY`
- Produces: handlers for `HttpMessageNotReadableException`, `MethodArgumentTypeMismatchException`

- [ ] `INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문이 올바르지 않습니다.")`를 추가한다.
- [ ] `HttpMessageNotReadableException` handler를 추가한다.
- [ ] `MethodArgumentTypeMismatchException` handler를 추가한다.
- [ ] 두 handler가 `ResponseEntity.status(400).body(CommonResponse.fail(ErrorCode.INVALID_REQUEST_BODY))`를 반환하게 한다.

### Task 3: 검증과 커밋

**Files:**
- All changed files from Tasks 1-2

- [ ] 타깃 테스트를 실행한다.
- [ ] `./gradlew test`를 실행한다.
- [ ] `AGENTS.md`, `gradle.properties`를 제외하고 stage한다.
- [ ] `feat: add invalid request body handling`으로 커밋한다.
- [ ] `develop`에 push한다.
- [ ] PR 설명에 `INVALID_REQUEST_BODY` 공통 처리와 테스트 항목을 추가한다.
