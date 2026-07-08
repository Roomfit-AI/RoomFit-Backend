# Room Furniture Update API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `PUT /api/rooms/{roomId}/furniture`가 명세 기준 status enum과 furniture id 검증을 수행하게 한다.

**Architecture:** 기존 `RoomController`, `RoomService`, `FurnitureUpdateRequest`, `RoomResponse`를 유지한다. 구현 변경은 `RoomService.updateFurnitureStatus()`에 집중하고, 테스트는 MockMvc 통합 테스트로 검증한다.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, MockMvc, in-memory Map Repository

## Global Constraints

- API 응답 body는 `success`, `data`, `error`만 포함한다.
- JSON enum 값은 명세 기준 대문자 enum을 사용한다.
- 명세에 없는 요청/응답 필드는 추가하지 않는다.
- 인증/인가는 MVP 범위에서 제외한다.

---

### Task 1: Room Furniture Update 실패 테스트

**Files:**
- Create: `src/test/java/com/roomfit/room/controller/RoomFurnitureControllerTest.java`

**Interfaces:**
- Consumes: `PUT /api/rooms/{roomId}/furniture`
- Produces: room furniture update API 성공/실패 케이스 회귀 테스트

- [ ] 정상 status 변경 테스트를 작성한다.
- [ ] 없는 roomId가 `ROOM_NOT_FOUND`를 반환하는 테스트를 작성한다.
- [ ] 없는 furniture id가 `FURNITURE_NOT_FOUND`를 반환하는 테스트를 작성한다.
- [ ] 소문자 status가 `INVALID_FURNITURE_STATUS`를 반환하는 테스트를 작성한다.
- [ ] `./gradlew test --tests com.roomfit.room.controller.RoomFurnitureControllerTest`로 RED를 확인한다.

### Task 2: Service 검증 구현

**Files:**
- Modify: `src/main/java/com/roomfit/room/RoomService.java`

**Interfaces:**
- Consumes: `FurnitureUpdateRequest.getFurnitureUpdates()`
- Produces: strict status parsing and unknown furniture validation

- [ ] room furniture id 집합을 만든다.
- [ ] 요청 id 중 room에 없는 id가 있으면 `FURNITURE_NOT_FOUND`를 던진다.
- [ ] `FurnitureStatus.valueOf(rawStatus)`로 대문자 enum만 허용한다.
- [ ] 파싱 실패 시 `INVALID_FURNITURE_STATUS`를 던진다.
- [ ] 요청에 포함된 furniture만 상태를 변경한다.

### Task 3: 검증과 커밋

**Files:**
- All changed files from Tasks 1-2

- [ ] 타깃 테스트를 실행한다.
- [ ] `./gradlew test`를 실행한다.
- [ ] `AGENTS.md`, `gradle.properties`를 제외하고 stage한다.
- [ ] `feat: harden room furniture update api`로 커밋한다.
- [ ] `develop`에 push한다.
- [ ] PR 설명에 room furniture update API 보강과 테스트 항목을 추가한다.
