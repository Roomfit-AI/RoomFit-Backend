# Room Furniture Update API Design

## 목표

`PUT /api/rooms/{roomId}/furniture`는 샘플 Room의 기존 가구 상태를 저장한다. MVP에서는 사용자가 기존 가구를 유지하거나 삭제하는 흐름을 지원하며, 응답은 `CommonResponse<RoomResponse>` 구조를 유지한다.

## 범위

- 포함: `PUT /api/rooms/{roomId}/furniture`
- 제외: `GET /api/rooms/{roomId}` 테스트 보강, layout update/validate 로직 변경

## 요청

기존 DTO인 `FurnitureUpdateRequest`를 유지한다.

```json
{
  "furnitureUpdates": [
    {
      "id": "desk-1",
      "status": "DELETED"
    }
  ]
}
```

## 처리 규칙

- 존재하지 않는 roomId는 `ROOM_NOT_FOUND / 404`를 반환한다.
- 존재하지 않는 furniture id가 요청에 포함되면 `FURNITURE_NOT_FOUND / 404`를 반환한다.
- status는 명세 기준 대문자 enum만 허용한다.
- 잘못된 status는 `INVALID_FURNITURE_STATUS / 400`을 반환한다.
- 요청에 포함된 furniture만 상태를 변경하고, 포함되지 않은 furniture는 기존 상태를 유지한다.

## 테스트

- 정상 요청은 `success=true`와 변경된 furniture status를 반환한다.
- 없는 roomId는 `ROOM_NOT_FOUND`를 반환한다.
- 없는 furniture id는 `FURNITURE_NOT_FOUND`를 반환한다.
- 소문자 또는 알 수 없는 status는 `INVALID_FURNITURE_STATUS`를 반환한다.
