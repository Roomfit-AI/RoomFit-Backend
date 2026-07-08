# Layout Feedback API Design

## 목표

`POST /api/layouts/feedback`는 기존 layout과 사용자의 대표 자연어 피드백을 기반으로 재추천 결과를 반환한다. MVP에서는 LLM이나 자유 자연어 처리를 사용하지 않고, 명세에 정의된 3개 문장만 지원한다.

## 범위

- 포함: `POST /api/layouts/feedback`
- 제외: LLM API 연동, 자유 자연어 해석, 실제 쇼핑몰 제품 재추천

## 요청

요청 body는 다음 두 필드만 사용한다.

- `layoutId`
- `feedback`

지원 문장은 다음과 같다.

- `책상 더 크게`
- `수납 늘려줘`
- `방이 넓어 보이게`

지원하지 않는 문장은 `UNSUPPORTED_FEEDBACK_INTENT / 400`으로 처리한다.

## 응답

응답 DTO는 recommend 응답과 같은 필드를 갖되 `interpretedIntent`를 추가한 별도 `FeedbackResponse`를 사용한다. 기존 `LayoutResponse`에 nullable `interpretedIntent`를 추가하지 않아 recommend/update 응답에 명세 밖 필드가 섞이지 않게 한다.

필드:

- `layoutId`
- `status`
- `recommendedFurniture`
- `scoreSummary`
- `validationResult`
- `interpretedIntent`

## 처리 규칙

- `책상 더 크게`: `interpretedIntent = { "deskMinWidth": 1.4 }`, desk 가구 width를 최소 1.4로 반영한다.
- `수납 늘려줘`: `interpretedIntent = { "storagePriority": "HIGH" }`, storage 가구가 있으면 크기를 확대하고 없으면 storage 추천 가구를 추가한다.
- `방이 넓어 보이게`: `interpretedIntent = { "openSpacePriority": "HIGH" }`, 추천 가구를 벽면 쪽 좌표로 재배치한다.

MVP에서는 새 재추천 layout을 생성해 저장하고, 응답의 `layoutId`는 새 layout id를 반환한다.

## 테스트

- `책상 더 크게` 요청은 `deskMinWidth=1.4`와 width 1.4 이상 desk를 반환한다.
- `수납 늘려줘` 요청은 `storagePriority=HIGH`를 반환한다.
- `방이 넓어 보이게` 요청은 `openSpacePriority=HIGH`를 반환한다.
- 지원하지 않는 피드백은 `UNSUPPORTED_FEEDBACK_INTENT / 400`을 반환한다.
- 없는 layoutId는 `LAYOUT_NOT_FOUND / 404`를 반환한다.
