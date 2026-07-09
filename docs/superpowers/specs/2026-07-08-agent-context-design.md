# Agent Context API 설계

## 범위

이번 단계에서는 `POST /api/agent/context`를 최종 API 명세서 v1.0 기준으로 구현한다. 이 API는 사용자의 방, 생활 목적, 디자인 취향, 필수/선택 가구 타입, 선택 취향 이미지, 선택 Mock Product 정보를 하나의 Agent Context로 저장하고 이후 추천 API에서 참조할 `contextId`를 발급한다.

이번 범위에서는 배치 추천, 배치 검증, 배치 수정, 자연어 피드백, 최종 확정, LLM 연동, RoomPlan 업로드, 실제 쇼핑몰 연동을 구현하지 않는다.

## 기준 문서

endpoint, request body, response body, HTTP status, ErrorCode, enum/허용값, 필드명은 최종 RoomFit AI 백엔드 API 명세서 v1.0을 최우선 기준으로 따른다.

폴더 구조는 사용자가 지정한 참고 저장소처럼 도메인 패키지 아래에 `controller`, `domain`, `dto/request`, `dto/response`, `repository`, `service` 하위 패키지를 두는 방식을 적용한다.

## 선행 의존성

Agent Context 생성에는 앞 단계에서 만든 Style Image와 Mock Product 데이터 조회가 필요하다.

- `StyleImageRepository.findById(Long imageId)`를 추가한다.
- `MockProductRepository.findById(String productId)`를 추가한다.
- `ErrorCode`에 명세서에서 요구하는 Agent Context 관련 에러를 추가한다.

이 의존성을 구현하지 않으면 `selectedImageIds`와 `selectedProductIds` 검증, `styleTags` 수집, `selectedProducts` 저장을 명세대로 처리할 수 없다. 따라서 이번 API의 최소 구현 범위에 포함한다.

## 접근안

추천 접근은 기존 `agent` 패키지를 참고 저장소 스타일에 맞춰 정리하면서 명세 기반 구현으로 바꾸는 방식이다.

- `agent/controller`: `AgentContextController`
- `agent/domain`: `AgentContext`, `LifestyleGoal`, `DesignStyle`
- `agent/dto/request`: `AgentContextRequest`
- `agent/dto/response`: `AgentContextResponse`, `SelectedProductResponse`, `RequiredClearanceResponse`
- `agent/repository`: `AgentContextRepository`
- `agent/service`: `AgentContextService`

대안으로 기존 flat `agent` 패키지를 유지하는 방식이 있지만, 이후 기능이 늘어날수록 DTO/domain/service 책임이 섞인다. 또 별도 `agentcontext` 패키지를 만드는 방식은 기존 `agent` 코드와 중복된다. 따라서 이번 단계에서 `agent` 내부 구조를 정리한다.

## API 계약

Endpoint:

`POST /api/agent/context`

성공 응답:

- HTTP Status: `201`
- Body: `CommonResponse<AgentContextResponse>`
- `success`: `true`
- `data`: 생성된 Agent Context
- `error`: `null`

Request 필드:

- `roomId`
- `lifestyleGoal`
- `designStyle`
- `requiredItems`
- `optionalItems`
- `selectedImageIds`
- `selectedProductIds`

Response 필드:

- `contextId`
- `roomId`
- `lifestyleGoal`
- `designStyle`
- `requiredItems`
- `optionalItems`
- `selectedImageIds`
- `selectedProductIds`
- `styleTags`
- `selectedProducts`
- `createdAt`

`styleTags`는 선택 이미지의 `tags`와 선택 제품의 `styleTags`를 합쳐 만든다. 중복 태그는 제거한다.

`selectedProducts`는 Agent Context 응답에서 명세서가 보여주는 필드만 포함한다.

- `productId`
- `type`
- `name`
- `width`
- `depth`
- `height`
- `styleTags`
- `requiredClearance.front`
- `requiredClearance.side`

## 검증 규칙

- `roomId`가 존재하지 않으면 `ROOM_NOT_FOUND / 404`
- `requiredItems`가 없거나 비어 있으면 `REQUIRED_ITEM_EMPTY / 400`
- `selectedImageIds`가 없거나 비어 있으면 `STYLE_IMAGE_EMPTY / 400`
- 존재하지 않는 imageId가 있으면 `STYLE_IMAGE_NOT_FOUND / 404`
- 존재하지 않는 productId가 있으면 `PRODUCT_NOT_FOUND / 404`
- `lifestyleGoal`이 허용값이 아니면 `INVALID_LIFESTYLE_GOAL / 400`
- `designStyle` 값 중 허용값이 아니면 `INVALID_DESIGN_STYLE / 400`
- `requiredItems` 또는 `optionalItems` 값 중 허용 가구 타입이 아니면 `INVALID_FURNITURE_TYPE / 400`

허용 값:

- `LifestyleGoal`: `STUDY_FOCUSED`, `RELAX_FOCUSED`, `STORAGE_FOCUSED`, `WFH_FOCUSED`
- `DesignStyle`: `MINIMAL`, `NATURAL`, `WHITE_TONE`, `WOOD_TONE`, `COZY`, `MODERN`
- `FurnitureType`: `bed`, `desk`, `chair`, `storage`, `rug`, `lamp`

## 데이터 흐름

Controller는 request를 Service로 넘기고 `CommonResponse.ok(response)`를 반환한다. Service는 Room, Style Image, Mock Product repository를 조회해 요청 값을 검증한다. 검증을 통과하면 선택 이미지 태그와 선택 제품 태그를 합쳐 `styleTags`를 만들고, 선택 제품 domain 객체를 Agent Context의 `selectedProducts`에 저장한다. Repository는 생성된 Agent Context에 in-memory id를 부여하고 저장한다.

`selectedProductIds`는 선택값이다. 누락되거나 빈 배열이면 제품 검증과 제품 태그 수집을 하지 않고 `selectedProductIds=[]`, `selectedProducts=[]`로 저장한다.

`optionalItems`도 선택값이다. 누락되면 빈 배열로 저장한다.

## 테스트

Controller 통합 테스트를 우선 작성한다.

- 정상 요청 시 HTTP 201, `success=true`, `error=null`
- 응답에 `contextId`, `roomId`, `lifestyleGoal`, `designStyle`, `requiredItems`, `optionalItems`, `selectedImageIds`, `selectedProductIds`, `styleTags`, `selectedProducts`, `createdAt` 포함
- 이미지 태그와 제품 태그가 `styleTags`에 합쳐짐
- `selectedProducts`에 제품 크기와 `requiredClearance`가 포함됨
- 존재하지 않는 roomId는 `ROOM_NOT_FOUND / 404`
- 빈 `requiredItems`는 `REQUIRED_ITEM_EMPTY / 400`
- 빈 `selectedImageIds`는 `STYLE_IMAGE_EMPTY / 400`
- 존재하지 않는 imageId는 `STYLE_IMAGE_NOT_FOUND / 404`
- 존재하지 않는 productId는 `PRODUCT_NOT_FOUND / 404`
- 잘못된 `lifestyleGoal`은 `INVALID_LIFESTYLE_GOAL / 400`
- 잘못된 `designStyle`은 `INVALID_DESIGN_STYLE / 400`
- 잘못된 가구 타입은 `INVALID_FURNITURE_TYPE / 400`

구현 후 전체 테스트를 실행한다.

## 제외 항목

- 추천 로직에서 `selectedProducts` 크기 우선 반영
- `status=DELETED` 기존 가구 제외 로직
- Swagger/OpenAPI 상세 예시 작성
- Bean Validation annotation 기반 전환
- GlobalExceptionHandler의 일반 JSON 파싱 예외 처리

위 항목은 이후 추천/검증 API 또는 공통 예외 처리 단계에서 별도 기능으로 다룬다.
