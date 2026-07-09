# Room Samples API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `GET /api/rooms/samples`가 샘플 Room JSON 목록을 `CommonResponse<List<RoomResponse>>`로 반환하게 한다.

**Architecture:** 기존 `RoomController`, `RoomService`, `RoomRepository`, `RoomResponse`를 재사용한다. Repository에 `findAll()`만 추가하고, 별도 summary DTO는 만들지 않는다.

**Tech Stack:** Java 17, Spring Boot, JUnit 5, MockMvc, in-memory Map Repository

## Global Constraints

- API 응답 body는 `success`, `data`, `error`만 포함한다.
- 명세에 없는 필드는 추가하지 않는다.
- 샘플 Room JSON은 기존 `RoomResponse` 구조를 따른다.
- Repository는 MVP 기준 in-memory Map 기반이다.

---

### Task 1: Room Samples 실패 테스트

**Files:**
- Create: `src/test/java/com/roomfit/room/controller/RoomSamplesControllerTest.java`

**Interfaces:**
- Consumes: `GET /api/rooms/samples`
- Produces: samples API 성공 케이스 회귀 테스트

- [ ] `GET /api/rooms/samples` 요청 테스트를 작성한다.
- [ ] `success=true`, `error=null`, `data` 배열을 검증한다.
- [ ] 첫 샘플의 `roomId`, `room`, `openings`, `furniture` 구조를 검증한다.
- [ ] `./gradlew test --tests com.roomfit.room.controller.RoomSamplesControllerTest`를 실행해 RED를 확인한다.

### Task 2: Repository/Service/Controller 구현

**Files:**
- Modify: `src/main/java/com/roomfit/room/RoomRepository.java`
- Modify: `src/main/java/com/roomfit/room/RoomService.java`
- Modify: `src/main/java/com/roomfit/room/RoomController.java`

**Interfaces:**
- Produces: `RoomRepository.findAll(): List<Room>`
- Produces: `RoomService.getSampleRooms(): List<RoomResponse>`

- [ ] `RoomRepository.findAll()`을 id 오름차순으로 구현한다.
- [ ] `RoomService.getSampleRooms()`를 추가한다.
- [ ] `RoomController.getSampleRooms()`를 `@GetMapping("/samples")`로 추가한다.
- [ ] 타깃 테스트를 다시 실행해 GREEN을 확인한다.

### Task 3: 검증과 커밋

**Files:**
- All changed files from Tasks 1-2

- [ ] `./gradlew test`를 실행한다.
- [ ] `AGENTS.md`, `gradle.properties`를 제외하고 stage한다.
- [ ] `feat: add room samples api`로 커밋한다.
- [ ] `develop`에 push한다.
- [ ] PR 설명에 room samples API와 테스트 항목을 추가한다.
