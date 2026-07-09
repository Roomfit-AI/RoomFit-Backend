# Layout Confirm API Design

## 목표

`POST /api/layouts/{layoutId}/confirm`은 사용자가 최종 배치를 확정하는 API이다. 이미 구현된 엔드포인트의 성공/실패 응답을 명세 기준 테스트로 고정한다.

## 범위

- 포함: `POST /api/layouts/{layoutId}/confirm`
- 제외: layout update, feedback, Swagger/OpenAPI 문서화

## 응답

성공 응답은 `CommonResponse<ConfirmResponse>` 구조를 따른다.

- `layoutId`
- `confirmed`
- `confirmedAt`

실패 응답은 다음을 따른다.

- 존재하지 않는 layoutId: `LAYOUT_NOT_FOUND / 404`
- 이미 확정된 layout 재확정: `ALREADY_CONFIRMED / 409`

## 테스트

- 정상 확정은 `success=true`, `confirmed=true`, `confirmedAt`을 반환한다.
- 없는 layoutId는 `LAYOUT_NOT_FOUND`를 반환한다.
- 같은 layout을 두 번 확정하면 두 번째 요청은 `ALREADY_CONFIRMED`를 반환한다.
