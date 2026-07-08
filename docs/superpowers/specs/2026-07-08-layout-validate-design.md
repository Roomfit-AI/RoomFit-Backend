# Layout Validate API Design

## 목표

`POST /api/layouts/validate`는 프론트의 현재 화면에 있는 전체 furniture 배열을 기준으로 배치 검증 결과를 반환한다. 응답은 기존 `CommonResponse<T>` 구조를 유지하고, `validationResult`는 명세의 체크리스트 렌더링 필드인 `boundaryValid`, `validationItems`, `warnings`를 포함한다.

## 범위

- 포함: `POST /api/layouts/validate`
- 제외: `PUT /api/layouts/{layoutId}`, `POST /api/layouts/feedback`, 최종 확정 로직 변경
- 제외 사유: update API는 validate의 배열 일치 검사와 검증 로직을 재사용하지만, 저장/확정 여부 처리까지 포함하므로 다음 단계에서 별도로 구현한다.

## 요청 처리

기존 `ValidateRequest`와 `FurniturePositionDto`를 유지한다. 요청의 `furniture` 배열은 현재 화면의 전체 가구 배열로 해석하며, 서버는 기존 layout의 furniture id 집합과 요청 id 집합을 비교한다.

- layoutId가 없으면 `LAYOUT_NOT_FOUND`
- 요청에 layout에 없는 furniture id가 있으면 `FURNITURE_NOT_FOUND`
- 기존 layout의 furniture id가 요청에서 누락되면 `FURNITURE_ARRAY_MISMATCH`
- position이 방 범위를 벗어나면 `INVALID_FURNITURE_POSITION`
- rotation이 0 이상 360 미만이 아니면 `INVALID_ROTATION`

## 검증 기준

- 충돌: 회전을 반영하지 않는 x-z AABB 겹침으로 판단한다.
- 방 범위: 중심 좌표와 width/depth의 half extent가 room width/depth 안에 있으면 통과한다.
- 문 앞 확보: MVP에서는 가구 중심이 문 opening의 확보 영역 안에 들어오면 실패한다.
- 창문 앞 확보: MVP에서는 가구 중심이 창문 확보 영역 안에 있고 가구 height가 sillHeight 이상이면 실패한다.
- 동선: MVP에서는 room 중앙 세로 통로 폭 `MIN_PATH_WIDTH = 0.6m`를 기준으로, 가구가 통로를 가로막는 명확한 케이스를 실패 처리한다.

## 테스트

컨트롤러 통합 테스트로 다음을 검증한다.

- 정상 validate 요청은 `success=true`와 명세 필드가 있는 `validationResult`를 반환한다.
- 충돌 위치를 보내면 `collisionFree=false`와 `validationItems.type=collision`의 `passed=false`가 반환된다.
- layout에 없는 furniture id는 `FURNITURE_NOT_FOUND / 404`를 반환한다.
- 기존 furniture id 누락은 `FURNITURE_ARRAY_MISMATCH / 400`을 반환한다.
- 방 범위 밖 좌표는 `INVALID_FURNITURE_POSITION / 400`을 반환한다.
- 잘못된 rotation은 `INVALID_ROTATION / 400`을 반환한다.
