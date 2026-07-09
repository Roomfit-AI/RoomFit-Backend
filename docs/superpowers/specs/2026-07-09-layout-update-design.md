# Layout Update API Design

## 목표

`PUT /api/layouts/{layoutId}`는 사용자가 화면에서 수정한 전체 furniture 배열을 저장하고, 저장된 배열 기준으로 다시 검증한 결과를 반환한다. 응답은 기존 `LayoutResponse`를 사용하며, body는 `CommonResponse<T>` 구조를 따른다.

## 범위

- 포함: `PUT /api/layouts/{layoutId}`
- 제외: 자연어 피드백 재추천, 최종 확정 API 보강, 방 조회/가구 상태 API 보강

## 요청 처리

요청 body는 기존 `LayoutUpdateRequest`의 `furniture` 배열을 사용한다. 배열은 현재 화면의 전체 가구 목록으로 해석한다.

- layoutId가 없으면 `LAYOUT_NOT_FOUND`
- layout이 이미 확정되어 있으면 `ALREADY_CONFIRMED`
- 요청에 기존 layout에 없는 furniture id가 있으면 `FURNITURE_NOT_FOUND`
- 기존 layout의 furniture id가 요청에서 누락되면 `FURNITURE_ARRAY_MISMATCH`
- position이 방 범위를 벗어나면 `INVALID_FURNITURE_POSITION`
- rotation이 0 이상 360 미만이 아니면 `INVALID_ROTATION`
- status가 요청에 포함되고 enum 값이 아니면 `INVALID_FURNITURE_STATUS`

## 상태 처리

요청 항목의 `status`가 있으면 명세의 대문자 enum 기준으로 반영한다. 사용자가 수정한 가구는 프론트가 `USER_MODIFIED`를 보낼 수 있으며, 서버는 이를 저장된 layout furniture에 반영한다. status가 없으면 기존 status를 유지한다.

## 응답

성공 시 `success=true`이고 `data`는 다음을 포함한다.

- `layoutId`
- `status`: update 응답에서는 `null`
- `recommendedFurniture`: 저장된 전체 furniture 배열
- `scoreSummary`: update 응답에서는 `null`
- `validationResult`

## 테스트

- 정상 update 요청은 수정된 position, rotation, status를 저장하고 `validationResult`를 반환한다.
- 없는 layoutId는 `LAYOUT_NOT_FOUND / 404`를 반환한다.
- 확정된 layout 수정은 `ALREADY_CONFIRMED / 409`를 반환한다.
- 잘못된 status는 `INVALID_FURNITURE_STATUS / 400`을 반환한다.
- 기존 validate 단계에서 보장된 배열 불일치 검증은 update에서도 유지한다.
