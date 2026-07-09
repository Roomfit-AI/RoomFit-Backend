# Style Images API 설계

## 범위

두 번째 독립 백엔드 기능으로 `GET /api/styles/images`만 구현한다. 이 API는 사용자가 취향 이미지 선택 UI에서 고를 수 있는 이미지 카드 목록을 반환한다.

이번 범위에서는 Agent Context 생성, `selectedImageIds` 검증, `styleTags` 통합 저장, Mock Product 조회 변경, 배치 추천, 배치 검증, 배치 수정, 자연어 피드백, 최종 확정, 인증/인가를 구현하지 않는다.

## 기준 문서

endpoint, 응답 래퍼, 필드명, 데이터 예시는 최종 RoomFit AI 백엔드 API 명세서 v1.0을 최우선 기준으로 따른다.

사용자가 참고하라고 지정한 Dcom intranet server 저장소의 폴더 구조를 반영해, 새 기능은 도메인 패키지 아래에 `controller`, `domain`, `dto/response`, `repository`, `service` 하위 패키지를 두는 방식으로 만든다.

## 접근안

추천 접근은 `com.roomfit.style` 독립 도메인 패키지를 추가하는 방식이다. 취향 이미지 목록은 이후 `POST /api/agent/context`에서 `selectedImageIds` 검증과 `styleTags` 수집에 재사용되어야 하므로, `agent` 패키지나 Controller 내부 정적 리스트에 넣지 않는다.

대안으로는 `agent` 패키지 안에 취향 이미지 데이터를 두는 방식과 Controller에서 정적 리스트를 바로 반환하는 방식이 있다. 하지만 전자는 취향 이미지 조회 책임과 Agent Context 생성 책임이 섞이고, 후자는 다음 단계에서 repository/service를 다시 분리해야 하므로 이번 설계에서는 제외한다.

## 패키지 구조

`com.roomfit.style` 아래에 새 style 도메인 패키지를 만든다.

- `style/controller`: Spring MVC endpoint 클래스
- `style/domain`: `StyleImage` 모델 객체
- `style/dto/response`: API 응답 DTO
- `style/repository`: in-memory Style Image 조회 저장소
- `style/service`: 읽기 전용 Style Image 조회 서비스

이번 범위에서는 기존 `room`, `agent`, `placement`, `product` 패키지를 수정하지 않는다.

## API 계약

Endpoint:

`GET /api/styles/images`

성공 응답:

- HTTP Status: `200`
- Body: `CommonResponse<List<StyleImageResponse>>`
- `success`: `true`
- `data`: Style Image 목록
- `error`: `null`

각 style image 응답에는 명세서에 정의된 필드만 포함한다.

- `imageId`
- `title`
- `imageUrl`
- `tags`

명세서에는 이 읽기 전용 목록 API에 대한 별도 실패 케이스가 정의되어 있지 않으므로, 이번 구현에서는 endpoint 전용 ErrorCode를 추가하지 않는다.

## Style Image 데이터

최종 API 명세서 예시에 나온 3개 이미지 데이터를 그대로 제공한다.

- `imageId: 1`, `title: "화이트톤 미니멀 원룸"`, `imageUrl: "/images/styles/minimal-white-1.jpg"`, `tags: ["minimal", "white_tone", "open_space"]`
- `imageId: 2`, `title: "내추럴 우드톤 원룸"`, `imageUrl: "/images/styles/natural-wood-1.jpg"`, `tags: ["natural", "wood_tone", "cozy"]`
- `imageId: 3`, `title: "공부형 원룸 인테리어"`, `imageUrl: "/images/styles/study-focused-1.jpg"`, `tags: ["study", "desk_zone", "minimal"]`

명세서에 없는 이미지, 태그, 필드는 추가하지 않는다.

## 데이터 흐름

Controller는 Service에 조회를 위임한다. Service는 Repository에서 전체 Style Image 목록을 읽고 domain 객체를 response DTO로 변환한다. Controller는 변환된 데이터를 `CommonResponse.ok(data)`로 감싸 반환한다.

Repository는 이번 범위에서 읽기 전용이며, 불변 in-memory list로 데이터를 보관해도 된다. `findById` 메서드는 Agent Context 구현 단계에서 `selectedImageIds` 검증이 필요해질 때 추가한다.

## 테스트

`GET /api/styles/images`에 대한 MVC 테스트를 추가한다.

- HTTP 200을 반환한다.
- `success=true`를 반환한다.
- `error=null`을 반환한다.
- 응답 배열 길이가 3이다.
- `imageId` 1, 2, 3을 포함한다.
- 각 항목이 `title`, `imageUrl`, `tags`를 포함한다.
- 명세서 예시 태그인 `minimal`, `white_tone`, `open_space`, `natural`, `wood_tone`, `cozy`, `study`, `desk_zone`을 포함한다.

구현 후 전체 테스트를 실행한다.

## 결정된 사항

사용자는 Mock Product 다음 단계로 Style Image API를 진행하는 데 동의했다. 데이터는 최종 API 명세서 예시 3개를 그대로 사용한다.
