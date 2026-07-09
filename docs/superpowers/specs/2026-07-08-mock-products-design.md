# Mock Product API 설계

## 범위

첫 번째 독립 백엔드 기능으로 `GET /api/products/mock`만 구현한다. 이 API는 사용자가 선택할 수 있는 MVP Mock Product 카드 목록을 반환하며, 지원하는 모든 가구 타입인 `bed`, `desk`, `chair`, `storage`, `rug`, `lamp`를 각각 최소 1개 이상 포함한다.

이번 범위에서는 취향 이미지 목록, Agent Context 생성, 배치 추천, 배치 검증, 배치 수정, 자연어 피드백, 최종 확정, 인증/인가, 실제 쇼핑몰 연동, RoomPlan 업로드, LLM 호출을 구현하지 않는다.

## 기준 문서

endpoint, 응답 래퍼, 필드명, 허용 가구 타입 값은 최종 RoomFit AI 백엔드 API 명세서 v1.0을 최우선 기준으로 따른다.

사용자가 참고하라고 지정한 Dcom intranet server 저장소는 폴더 구조 참고용으로만 사용한다. 해당 저장소의 주요 패턴은 도메인 패키지 아래에 `controller`, `domain`, `dto/request`, `dto/response`, `repository`, `service` 같은 역할별 하위 패키지를 두는 방식이다.

## 패키지 구조

`com.roomfit.product` 아래에 새 product 도메인 패키지를 만든다.

- `product/controller`: Spring MVC endpoint 클래스
- `product/domain`: `MockProduct`, `RequiredClearance` 같은 product 모델 객체
- `product/dto/response`: API 응답 DTO
- `product/repository`: in-memory Mock Product 조회 저장소
- `product/service`: 읽기 전용 product 조회 서비스

이번 범위에서는 기존 `room`, `agent`, `placement` 패키지에 product 책임을 넣지 않는다.

## API 계약

Endpoint:

`GET /api/products/mock`

성공 응답:

- HTTP Status: `200`
- Body: `CommonResponse<List<MockProductResponse>>`
- `success`: `true`
- `data`: Mock Product 목록
- `error`: `null`

각 product 응답에는 명세서에 정의된 필드만 포함한다.

- `productId`
- `type`
- `name`
- `brand`
- `width`
- `depth`
- `height`
- `price`
- `styleTags`
- `imageUrl`
- `requiredClearance.front`
- `requiredClearance.side`

명세서에는 이 읽기 전용 목록 API에 대한 별도 실패 케이스가 정의되어 있지 않으므로, 이번 구현에서는 endpoint 전용 ErrorCode를 추가하지 않는다.

## Mock 데이터

MVP 가구 타입별로 1개씩, 총 6개의 Mock Product를 in-memory 데이터로 제공한다. 명세서 예시에 나온 다음 product id는 그대로 유지한다.

- `desk-01`
- `chair-01`
- `lamp-01`

아래 타입은 명세 필드만 사용해 최소 Mock Product를 추가한다.

- `bed`
- `storage`
- `rug`

추가 product도 명세서의 허용 가구 타입 값과 정의된 필드만 사용한다.

## 데이터 흐름

Controller는 Service에 조회를 위임한다. Service는 Repository에서 전체 Mock Product 목록을 읽고 domain 객체를 response DTO로 변환한다. Controller는 변환된 데이터를 `CommonResponse.ok(data)`로 감싸 반환한다.

Repository는 이번 범위에서 읽기 전용이며, 불변 in-memory list로 데이터를 보관해도 된다. `findById` 메서드는 Agent Context 구현 단계에서 필요해질 때 추가한다.

## 테스트

`GET /api/products/mock`에 대한 MVC 테스트를 추가한다.

- HTTP 200을 반환한다.
- `success=true`를 반환한다.
- `error=null`을 반환한다.
- 6개 지원 가구 타입을 모두 포함한다.
- 중첩 필드 `requiredClearance.front`, `requiredClearance.side`를 포함한다.
- 예시 product id인 `desk-01`, `chair-01`, `lamp-01`을 유지한다.

구현 후 전체 테스트를 실행한다.

## 결정된 사항

사용자는 MVP 전체 가구 타입 6개를 모두 커버하는 Mock Product 데이터 구성을 승인했다.
