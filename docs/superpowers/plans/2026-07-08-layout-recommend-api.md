# Layout Recommend API Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** `POST /api/layouts/recommend` API가 명세의 추천 응답 구조인 `recommendedFurniture`, `scoreSummary`, `validationResult`를 반환하도록 구현한다.

**Architecture:** 기존 `placement` 패키지 구조를 유지하면서 응답 DTO와 추천 결과 모델을 확장한다. `RuleBasedPlacementService`는 Agent Context의 selected products를 우선 반영하고, selected product가 없는 타입은 기본 크기 fallback을 사용한다. `ValidationService`는 이번 범위에서 명세 구조의 기본 통과 결과를 반환한다.

**Tech Stack:** Java 21, Spring Boot 3.5.16, Spring MVC, JUnit 5, Spring Boot Test, MockMvc.

## Global Constraints

- 최종 RoomFit AI 백엔드 API 명세서 v1.0을 endpoint, request body, response body, HTTP status, ErrorCode, enum/허용값, 필드명의 최우선 기준으로 삼는다.
- API 응답 body는 `success`, `data`, `error`만 포함하는 `CommonResponse<T>` 구조를 따른다.
- `POST /api/layouts/recommend`만 구현하고 validate/update/feedback/confirm 기능은 이번 범위에서 확장하지 않는다.
- 추천 응답 가구 배열 필드명은 반드시 `recommendedFurniture`다.
- 추천 응답에는 `scoreSummary`와 `validationResult`를 포함한다.
- 정밀 충돌/문/창문/동선 검증은 다음 `POST /api/layouts/validate`에서 구현한다.
- 명세서에 없는 응답 필드, enum 값, ErrorCode를 추가하지 않는다.

---

## 파일 구조

새로 생성할 파일:

- `src/main/java/com/roomfit/placement/ScoreSummary.java`
- `src/main/java/com/roomfit/placement/ValidationItem.java`
- `src/test/java/com/roomfit/placement/LayoutRecommendControllerTest.java`

수정할 파일:

- `src/main/java/com/roomfit/room/Furniture.java`
- `src/main/java/com/roomfit/placement/PlacementResult.java`
- `src/main/java/com/roomfit/placement/RuleBasedPlacementService.java`
- `src/main/java/com/roomfit/placement/ValidationResult.java`
- `src/main/java/com/roomfit/placement/ValidationService.java`
- `src/main/java/com/roomfit/placement/dto/LayoutResponse.java`

---

### Task 1: Layout Recommend 응답 계약 구현

**Files:**
- Test: `src/test/java/com/roomfit/placement/LayoutRecommendControllerTest.java`
- Create: `src/main/java/com/roomfit/placement/ScoreSummary.java`
- Create: `src/main/java/com/roomfit/placement/ValidationItem.java`
- Modify: `src/main/java/com/roomfit/room/Furniture.java`
- Modify: `src/main/java/com/roomfit/placement/PlacementResult.java`
- Modify: `src/main/java/com/roomfit/placement/RuleBasedPlacementService.java`
- Modify: `src/main/java/com/roomfit/placement/ValidationResult.java`
- Modify: `src/main/java/com/roomfit/placement/ValidationService.java`
- Modify: `src/main/java/com/roomfit/placement/dto/LayoutResponse.java`

**Interfaces:**
- Consumes:
  - `AgentContext.getRequiredItems()`
  - `AgentContext.getOptionalItems()`
  - `AgentContext.getSelectedProducts()`
  - `Room.getFurniture()`
- Produces:
  - `PlacementResult.getScoreSummary(): ScoreSummary`
  - `LayoutResponse.getScoreSummary(): ScoreSummary`
  - `ValidationResult.getValidationItems(): List<ValidationItem>`
  - `Furniture.getProductId(): String`
  - `Furniture.getStyleTags(): List<String>`

- [ ] **Step 1: 실패하는 Controller 테스트 작성**

`src/test/java/com/roomfit/placement/LayoutRecommendControllerTest.java`를 생성한다.

테스트는 먼저 Agent Context를 생성한 뒤 `POST /api/layouts/recommend`를 호출한다.

검증 항목:

- HTTP 201
- `success=true`, `error=null`
- `data.layoutId` 존재
- `data.status=SUCCESS`
- `data.recommendedFurniture` 존재
- `data.scoreSummary.totalScore` 존재
- `data.validationResult.boundaryValid` 존재
- `data.validationResult.validationItems`에 5개 항목 포함
- selected product `desk-01`이 추천 가구에 `productId`, 실제 크기, `styleTags`로 반영
- 존재하지 않는 contextId는 `CONTEXT_NOT_FOUND / 404`

- [ ] **Step 2: 테스트 실패 확인**

Run:

```bash
./gradlew test --tests com.roomfit.placement.LayoutRecommendControllerTest
```

Expected:

```text
BUILD FAILED
```

예상 실패 이유는 현재 응답에 `scoreSummary`, `boundaryValid`, `validationItems`, 추천 가구 `productId/styleTags`가 없기 때문이다.

- [ ] **Step 3: Furniture 응답 필드 확장**

`Furniture`에 optional 필드 `productId`, `styleTags`를 추가한다. 기존 생성자는 유지하고, product 기반 추천 가구를 만들 수 있는 생성자를 추가한다.

- [ ] **Step 4: ScoreSummary 추가**

`ScoreSummary`를 추가한다. MVP 기본 하위 점수는 100, 100, 90, 90, 90, 85로 두고 `totalScore`는 합산값으로 만든다.

- [ ] **Step 5: ValidationItem과 ValidationResult 확장**

`ValidationItem`을 추가한다. `ValidationResult`에는 `boundaryValid`, `validationItems`, `warnings`를 포함한다.

`ValidationService`는 이번 범위에서 명세 체크리스트 5개를 모두 passed=true로 반환한다.

- [ ] **Step 6: PlacementResult/LayoutResponse 확장**

`PlacementResult`와 `LayoutResponse`에 `scoreSummary`를 추가한다. 추천 응답에서는 scoreSummary가 포함되고, update 응답에서는 기존 동작 보존을 위해 null 가능하게 둔다.

- [ ] **Step 7: RuleBasedPlacementService 추천 로직 확장**

추천 결과에 room의 기존 furniture 중 `EXISTING`만 먼저 포함한다. `DELETED`는 제외한다.

requiredItems와 optionalItems를 순서대로 추천 가구로 만든다.

- 선택 제품 중 type이 일치하는 제품이 있으면 제품 크기, 이름, productId, styleTags 사용
- 없으면 type별 기본 크기 fallback 사용
- 추천 가구 status는 `RECOMMENDED`

- [ ] **Step 8: 대상 테스트 통과 확인**

Run:

```bash
./gradlew test --tests com.roomfit.placement.LayoutRecommendControllerTest
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

- `POST /api/layouts/recommend` 외 endpoint를 추가하지 않았다.
- 추천 응답 필드명이 `recommendedFurniture`다.
- `scoreSummary`가 추천 응답에 포함된다.
- `validationResult`가 `boundaryValid`, `validationItems`, `warnings`를 포함한다.
- `selectedProducts`가 추천 가구 크기와 `productId`, `styleTags`에 반영된다.
- 정밀 검증 알고리즘을 이번 범위에서 구현하지 않았다.

- [ ] **Step 11: 커밋**

Run:

```bash
git add src/main/java/com/roomfit/placement src/main/java/com/roomfit/room/Furniture.java src/test/java/com/roomfit/placement
git commit -m "feat: add layout recommend api response"
```

Expected:

```text
[develop <commit>] feat: add layout recommend api response
```

---

## 계획 자체 검토

- Spec coverage: recommend endpoint 응답 필드, selected product 반영, scoreSummary, validationResult 구조, CONTEXT_NOT_FOUND 테스트를 포함했다.
- Placeholder scan: 빈칸으로 남긴 구현 지시나 모호한 후속 작업 표현을 남기지 않았다.
- Type consistency: `ScoreSummary`, `ValidationItem`, `PlacementResult`, `LayoutResponse`의 필드와 getter 이름을 일관되게 사용한다.
