# Layout Recommend API 설계

## 범위

이번 단계에서는 `POST /api/layouts/recommend`를 최종 API 명세서 v1.0 기준 응답 계약에 맞게 구현한다. Agent Context를 기반으로 추천 배치 1개를 만들고, `layoutId`, `status`, `recommendedFurniture`, `scoreSummary`, `validationResult`를 반환한다.

이번 범위에서는 `POST /api/layouts/validate`의 정밀 검증 알고리즘, 배치 수정, 자연어 피드백, 최종 확정, LLM 연동을 구현하지 않는다. 다만 추천 응답에 포함되어야 하는 `validationResult` 구조는 명세 형태로 확장한다.

## 기준 문서

endpoint, request body, response body, HTTP status, ErrorCode, enum/허용값, 필드명은 최종 RoomFit AI 백엔드 API 명세서 v1.0을 최우선 기준으로 따른다.

## 선행 의존성

이 API는 이미 구현된 Agent Context를 사용한다.

- `AgentContextRepository.findById(contextId)`로 context를 조회한다.
- context가 없으면 `CONTEXT_NOT_FOUND / 404`를 반환한다.
- context의 `selectedProducts`가 있으면 추천 가구 크기와 `productId`, `styleTags`에 우선 반영한다.
- selected product가 없는 타입은 type별 기본 크기 fallback을 사용한다.

추천 결과에 기존 방 가구를 포함하려면 `RoomRepository`에서 room과 기존 furniture를 조회해야 한다.

## 접근안

추천 접근은 현재 `placement` 패키지를 유지하면서 응답 계약과 최소 추천 로직을 명세에 맞추는 방식이다.

포함 작업:

- `Furniture`에 응답용 optional 필드 `productId`, `styleTags` 추가
- `ScoreSummary` 추가
- `ValidationResult`에 `boundaryValid`, `validationItems`, `warnings` 추가
- `ValidationItem` 추가
- `LayoutResponse`에 `scoreSummary` 추가
- `PlacementResult`에 `scoreSummary` 포함
- `RuleBasedPlacementService`가 기존 `EXISTING` 가구와 추천 가구를 함께 반환
- `selectedProducts` 크기/productId/styleTags 우선 반영
- type별 fallback 크기 제공

대안으로 실제 충돌/문/창문/동선 검증 알고리즘까지 함께 구현할 수 있지만, 그러면 다음 `POST /api/layouts/validate`의 핵심 작업과 섞인다. 이번 단계에서는 추천 API 응답 계약과 데이터 반영을 먼저 안정화한다.

## API 계약

Endpoint:

`POST /api/layouts/recommend`

Request:

- `contextId`

성공 응답:

- HTTP Status: `201`
- Body: `CommonResponse<LayoutResponse>`
- `success`: `true`
- `data.layoutId`
- `data.status`
- `data.recommendedFurniture`
- `data.scoreSummary`
- `data.validationResult`
- `error`: `null`

실패 응답:

- context가 없으면 `CONTEXT_NOT_FOUND / 404`
- 추천 로직 실패 시 `RECOMMENDATION_FAILED / 500`

## 추천 로직 MVP

- `requiredItems`는 반드시 배치 시도한다.
- `optionalItems`는 이번 범위에서는 명세 응답 안정화를 위해 공간 계산 없이 순서대로 배치 시도하되, 추천 실패를 만들지 않는다.
- room의 기존 furniture 중 `status=EXISTING`인 가구는 결과에 포함한다.
- `status=DELETED`인 가구는 결과에서 제외한다.
- required/optional item type에 해당하는 selected product가 있으면 제품의 `width`, `depth`, `height`, `productId`, `styleTags`, `name`을 추천 가구에 반영한다.
- 해당 type의 selected product가 없으면 type별 기본 크기와 label을 사용한다.
- 추천 가구 `status`는 `RECOMMENDED`로 반환한다.
- 좌표는 MVP 고정 규칙으로 room 내부에 순차 배치한다.

## scoreSummary

`scoreSummary`는 추천 응답에 항상 포함한다.

필드:

- `totalScore`
- `collisionScore`
- `boundaryScore`
- `doorWindowScore`
- `pathScore`
- `goalScore`
- `styleScore`

MVP에서는 정밀 점수화 대신 기본 점수를 사용한다. 각 하위 점수는 0~100 범위이고, `totalScore`는 하위 점수 합산값이다.

## validationResult

추천 응답의 `validationResult`는 명세 구조를 따른다.

- `collisionFree`
- `boundaryValid`
- `doorClearance`
- `windowClearance`
- `pathSecured`
- `validationItems`
- `warnings`

이번 범위에서는 `ValidationService`가 구조를 완성하고 기본 통과 결과를 반환한다. 실제 충돌, boundary, 문/창문, 동선 실패 판정은 다음 `POST /api/layouts/validate`에서 구현한다.

`validationItems.type`은 명세 허용값만 사용한다.

- `collision`
- `boundary`
- `door_clearance`
- `window_clearance`
- `path`

## 테스트

Controller 통합 테스트를 우선 작성한다.

- 정상 요청 시 HTTP 201, `success=true`, `error=null`
- 응답에 `layoutId`, `status=SUCCESS`, `recommendedFurniture`, `scoreSummary`, `validationResult` 포함
- 추천 응답 필드명이 `recommendedFurniture`인지 확인
- `scoreSummary.totalScore`와 하위 점수 포함 확인
- `validationResult.boundaryValid`, `validationItems`, `warnings` 포함 확인
- selected product가 있는 `desk`는 `productId=desk-01`, 제품 크기, `styleTags`를 반영
- 기존 `EXISTING` 가구가 추천 결과에 포함
- context가 없으면 `CONTEXT_NOT_FOUND / 404`

구현 후 전체 테스트를 실행한다.

## 제외 항목

- 정밀 collision 검증
- 정밀 boundary 검증
- 문/창문 확보 영역 계산
- 동선 폭 계산
- optionalItems 공간 기반 생략 로직
- LLM/AI Agent fallback
- 후보별 점수 목록 반환
