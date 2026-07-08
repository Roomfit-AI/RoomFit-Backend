# Layout Update API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `PUT /api/layouts/{layoutId}`가 수정된 전체 furniture 배열을 저장하고 재검증 결과를 반환하게 한다.

**Architecture:** 기존 `LayoutController`와 `LayoutService.updateLayout()`를 유지한다. `LayoutService`의 기존 전체 배열 검증과 position/rotation 검증을 재사용하고, status 파싱만 추가해 저장된 `Furniture` 복사본에 반영한다.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, MockMvc, in-memory Map Repository

## Global Constraints

- API 응답 body는 `success`, `data`, `error`만 포함한다.
- `PUT /api/layouts/{layoutId}`는 현재 화면의 전체 furniture 배열을 받는다.
- layout에 없는 furniture id는 `FURNITURE_NOT_FOUND`로 처리한다.
- 기존 layout의 furniture id가 요청에서 누락되면 `FURNITURE_ARRAY_MISMATCH`로 처리한다.
- 이미 확정된 layout은 수정할 수 없다.
- JSON enum 값은 명세 기준 대문자 enum을 사용한다.

---

### Task 1: Update API 실패 테스트

**Files:**
- Create: `src/test/java/com/roomfit/placement/LayoutUpdateControllerTest.java`

**Interfaces:**
- Consumes: `POST /api/agent/context`, `POST /api/layouts/recommend`, `PUT /api/layouts/{layoutId}`, `POST /api/layouts/{layoutId}/confirm`
- Produces: update API 성공/실패 케이스 회귀 테스트

- [ ] 정상 update 요청에서 `recommendedFurniture`의 position, rotation, status가 변경되는 테스트를 작성한다.
- [ ] 없는 layoutId가 `LAYOUT_NOT_FOUND`를 반환하는 테스트를 작성한다.
- [ ] 확정된 layout 수정이 `ALREADY_CONFIRMED`를 반환하는 테스트를 작성한다.
- [ ] 잘못된 status가 `INVALID_FURNITURE_STATUS`를 반환하는 테스트를 작성한다.
- [ ] `./gradlew test --tests com.roomfit.placement.LayoutUpdateControllerTest`를 실행해 RED를 확인한다.

### Task 2: Status 파싱과 저장

**Files:**
- Modify: `src/main/java/com/roomfit/placement/LayoutService.java`

**Interfaces:**
- Consumes: `FurniturePositionDto.getStatus()`
- Produces: `Furniture` copy with updated `FurnitureStatus`

- [ ] `parseFurnitureStatus(String rawStatus, FurnitureStatus fallback)` helper를 추가한다.
- [ ] status가 null이면 기존 furniture status를 유지한다.
- [ ] status가 있으면 `FurnitureStatus.valueOf(rawStatus)`로 대문자 enum을 파싱한다.
- [ ] 파싱 실패 시 `CustomException(ErrorCode.INVALID_FURNITURE_STATUS)`를 던진다.
- [ ] `copyWithOverride()`에서 파싱한 status를 새 `Furniture`에 넣는다.

### Task 3: 검증과 커밋

**Files:**
- All changed files from Tasks 1-2

- [ ] `./gradlew test --tests com.roomfit.placement.LayoutUpdateControllerTest`를 실행한다.
- [ ] `./gradlew test`를 실행한다.
- [ ] `AGENTS.md`, `gradle.properties`를 제외하고 stage한다.
- [ ] `feat: add layout update api`로 커밋한다.
- [ ] `develop`에 push한다.
- [ ] PR 설명에 update API와 테스트 항목을 추가한다.
