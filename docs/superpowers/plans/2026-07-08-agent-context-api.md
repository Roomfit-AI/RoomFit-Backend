# Agent Context API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `POST /api/agent/context` API를 최종 명세서 기준으로 구현해 방/목적/취향/이미지/제품 선택 정보를 하나의 Agent Context로 저장하고 응답한다.

**Architecture:** 기존 flat `com.roomfit.agent` 패키지를 참고 저장소 스타일에 맞춰 `controller`, `domain`, `dto/request`, `dto/response`, `repository`, `service`로 재구성한다. Service는 Room, Style Image, Mock Product repository를 조회해 요청을 검증하고, 이미지 태그와 제품 태그를 합산해 `styleTags`를 만든 뒤 in-memory AgentContextRepository에 저장한다.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring MVC, JUnit 5, Spring Boot Test, MockMvc.

## Global Constraints

- 최종 RoomFit AI 백엔드 API 명세서 v1.0을 endpoint, request body, response body, HTTP status, ErrorCode, enum/허용값, 필드명의 최우선 기준으로 삼는다.
- API 응답 body는 `success`, `data`, `error`만 포함하는 `CommonResponse<T>` 구조를 따른다.
- `POST /api/agent/context`만 구현하고 추천, 검증, 수정, 피드백, 확정 기능은 구현하지 않는다.
- `selectedProductIds`는 optional이며 누락 또는 빈 배열이면 `selectedProductIds=[]`, `selectedProducts=[]`로 저장한다.
- `optionalItems`는 optional이며 누락이면 빈 배열로 저장한다.
- 명세서에 없는 응답 필드, enum 값, ErrorCode를 추가하지 않는다.
- 인증/인가는 MVP 범위에서 생략한다.
- 실제 쇼핑몰 제품 추천은 구현하지 않고 Mock Product 데이터만 사용한다.
- Swagger/OpenAPI 상세 예시는 이번 범위에서 제외한다.

---

## 파일 구조

이동할 파일:

- Move: `src/main/java/com/roomfit/agent/AgentContextController.java` -> `src/main/java/com/roomfit/agent/controller/AgentContextController.java`
- Move: `src/main/java/com/roomfit/agent/AgentContext.java` -> `src/main/java/com/roomfit/agent/domain/AgentContext.java`
- Move: `src/main/java/com/roomfit/agent/LifestyleGoal.java` -> `src/main/java/com/roomfit/agent/domain/LifestyleGoal.java`
- Move: `src/main/java/com/roomfit/agent/AgentContextRepository.java` -> `src/main/java/com/roomfit/agent/repository/AgentContextRepository.java`
- Move: `src/main/java/com/roomfit/agent/AgentContextService.java` -> `src/main/java/com/roomfit/agent/service/AgentContextService.java`
- Move: `src/main/java/com/roomfit/agent/dto/AgentContextRequest.java` -> `src/main/java/com/roomfit/agent/dto/request/AgentContextRequest.java`
- Move: `src/main/java/com/roomfit/agent/dto/AgentContextResponse.java` -> `src/main/java/com/roomfit/agent/dto/response/AgentContextResponse.java`

새로 생성할 파일:

- `src/main/java/com/roomfit/agent/domain/DesignStyle.java`
- `src/main/java/com/roomfit/agent/dto/response/SelectedProductResponse.java`
- `src/main/java/com/roomfit/agent/dto/response/RequiredClearanceResponse.java`
- `src/test/java/com/roomfit/agent/controller/AgentContextControllerTest.java`

수정할 파일:

- `src/main/java/com/roomfit/common/ErrorCode.java`
- `src/main/java/com/roomfit/style/repository/StyleImageRepository.java`
- `src/main/java/com/roomfit/product/repository/MockProductRepository.java`
- `src/main/java/com/roomfit/placement/LayoutService.java`
- `src/main/java/com/roomfit/placement/RuleBasedPlacementService.java`

---

### Task 1: Agent Context 명세 기반 생성 API

**Files:**
- Test: `src/test/java/com/roomfit/agent/controller/AgentContextControllerTest.java`
- Move/Modify: `src/main/java/com/roomfit/agent/controller/AgentContextController.java`
- Move/Modify: `src/main/java/com/roomfit/agent/domain/AgentContext.java`
- Move/Modify: `src/main/java/com/roomfit/agent/domain/LifestyleGoal.java`
- Create: `src/main/java/com/roomfit/agent/domain/DesignStyle.java`
- Move/Modify: `src/main/java/com/roomfit/agent/repository/AgentContextRepository.java`
- Move/Modify: `src/main/java/com/roomfit/agent/service/AgentContextService.java`
- Move/Modify: `src/main/java/com/roomfit/agent/dto/request/AgentContextRequest.java`
- Move/Modify: `src/main/java/com/roomfit/agent/dto/response/AgentContextResponse.java`
- Create: `src/main/java/com/roomfit/agent/dto/response/SelectedProductResponse.java`
- Create: `src/main/java/com/roomfit/agent/dto/response/RequiredClearanceResponse.java`
- Modify: `src/main/java/com/roomfit/common/ErrorCode.java`
- Modify: `src/main/java/com/roomfit/style/repository/StyleImageRepository.java`
- Modify: `src/main/java/com/roomfit/product/repository/MockProductRepository.java`
- Modify: `src/main/java/com/roomfit/placement/LayoutService.java`
- Modify: `src/main/java/com/roomfit/placement/RuleBasedPlacementService.java`

**Interfaces:**
- Consumes:
  - `RoomRepository.findById(Long id)`
  - `StyleImageRepository.findById(Long imageId): Optional<StyleImage>`
  - `MockProductRepository.findById(String productId): Optional<MockProduct>`
  - `AgentContextRepository.save(AgentContext context): AgentContext`
- Produces:
  - `AgentContextService.createContext(AgentContextRequest request): AgentContextResponse`
  - `AgentContextResponse.from(AgentContext context): AgentContextResponse`
  - `SelectedProductResponse.from(MockProduct product): SelectedProductResponse`
  - `RequiredClearanceResponse.from(RequiredClearance requiredClearance): RequiredClearanceResponse`

- [ ] **Step 1: 실패하는 Controller 테스트 작성**

`src/test/java/com/roomfit/agent/controller/AgentContextControllerTest.java`를 생성한다.

테스트는 정상 생성, 필수 값 검증, 존재하지 않는 참조 id, enum/타입 검증을 확인한다.

- [ ] **Step 2: 테스트 실패 확인**

Run:

```bash
./gradlew test --tests com.roomfit.agent.controller.AgentContextControllerTest
```

Expected:

```text
BUILD FAILED
```

예상 실패 이유는 기존 request/response 구조와 package 구조가 명세 테스트를 만족하지 못하기 때문이다.

- [ ] **Step 3: agent 패키지 구조 이동**

기존 agent 파일을 다음 패키지로 이동하고 package 선언과 import를 갱신한다.

- `com.roomfit.agent.controller`
- `com.roomfit.agent.domain`
- `com.roomfit.agent.repository`
- `com.roomfit.agent.service`
- `com.roomfit.agent.dto.request`
- `com.roomfit.agent.dto.response`

placement 코드의 `AgentContext`, `AgentContextRepository` import도 새 패키지로 변경한다.

- [ ] **Step 4: ErrorCode 보강**

`ErrorCode`에 아래 명세 코드만 추가한다.

- `STYLE_IMAGE_NOT_FOUND / 404`
- `PRODUCT_NOT_FOUND / 404`
- `INVALID_LIFESTYLE_GOAL / 400`
- `INVALID_DESIGN_STYLE / 400`
- `INVALID_FURNITURE_TYPE / 400`
- `STYLE_IMAGE_EMPTY / 400`

- [ ] **Step 5: Style/Product repository 조회 메서드 추가**

`StyleImageRepository.findById(Long imageId)`와 `MockProductRepository.findById(String productId)`를 추가한다.

- [ ] **Step 6: domain/DTO를 명세 구조로 확장**

`AgentContext`에 아래 필드를 저장한다.

- `selectedImageIds`
- `selectedProductIds`
- `selectedProducts`
- `styleTags`

`AgentContextRequest`는 `styleTags` 요청 필드를 제거하고 `selectedImageIds`, `selectedProductIds`를 받는다.

`AgentContextResponse`는 `selectedImageIds`, `selectedProductIds`, `selectedProducts`를 포함하고 `lifestyleGoal`은 대문자 enum 문자열로 응답한다.

- [ ] **Step 7: Service 검증 및 생성 로직 구현**

`AgentContextService.createContext`에서 아래를 처리한다.

- room 조회
- `requiredItems` 필수 검증
- `selectedImageIds` 필수 검증
- lifestyleGoal enum 검증
- designStyle 배열 enum 검증
- required/optional furniture type 검증
- style image 조회 및 tag 수집
- selected product 조회 및 tag 수집
- 중복 제거된 `styleTags` 생성
- context 저장 및 response 반환

- [ ] **Step 8: 대상 테스트 통과 확인**

Run:

```bash
./gradlew test --tests com.roomfit.agent.controller.AgentContextControllerTest
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 9: 전체 테스트 통과 확인**

Run:

```bash
./gradlew test
```

Expected:

```text
BUILD SUCCESSFUL
```

- [ ] **Step 10: 변경사항 검토**

검토 기준:

- `POST /api/agent/context` 외 endpoint를 추가하지 않았다.
- `CommonResponse` 응답 구조를 유지한다.
- `styleTags`는 이미지 태그와 제품 태그에서 생성된다.
- request에서 `styleTags`를 받지 않는다.
- `selectedProducts`는 명세 필드만 포함한다.
- 추천/검증 로직은 구현하지 않았다.
- 기존 product/style 조회 API가 계속 통과한다.

- [ ] **Step 11: 커밋**

Run:

```bash
git add src/main/java/com/roomfit/agent src/main/java/com/roomfit/common/ErrorCode.java src/main/java/com/roomfit/style/repository/StyleImageRepository.java src/main/java/com/roomfit/product/repository/MockProductRepository.java src/main/java/com/roomfit/placement/LayoutService.java src/main/java/com/roomfit/placement/RuleBasedPlacementService.java src/test/java/com/roomfit/agent
git commit -m "feat: add agent context api"
```

Expected:

```text
[develop <commit>] feat: add agent context api
```

---

## 계획 자체 검토

- Spec coverage: Agent Context request/response, selected images/products, styleTags 생성, ErrorCode, repository 의존성, 폴더 구조 변경, 테스트 요구사항을 포함했다.
- Placeholder scan: 빈칸으로 남긴 구현 지시나 모호한 후속 작업 표현을 남기지 않았다.
- Type consistency: 이동 후 패키지 경로와 Service/Repository/DTO 메서드 이름을 일관되게 사용한다.
