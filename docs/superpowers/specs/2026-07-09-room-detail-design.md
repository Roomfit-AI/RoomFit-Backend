# Room Detail API Design

## 목표

`GET /api/rooms/{roomId}`는 특정 Room JSON을 `CommonResponse<RoomResponse>`로 반환한다. 이미 구현된 엔드포인트의 명세 준수 여부를 테스트로 고정한다.

## 범위

- 포함: `GET /api/rooms/{roomId}` 정상/실패 응답 테스트
- 제외: `GET /api/rooms/samples`, `PUT /api/rooms/{roomId}/furniture`, 새로운 응답 DTO 추가

## 응답

성공 응답은 기존 `RoomResponse` 구조를 따른다.

- `roomId`
- `room.width`
- `room.depth`
- `room.height`
- `room.unit`
- `openings`
- `furniture`

실패 응답은 존재하지 않는 roomId에 대해 `ROOM_NOT_FOUND / 404`를 반환한다.

## 테스트

- 존재하는 roomId는 `200 OK`, `success=true`, `error=null`과 Room JSON 구조를 반환한다.
- 존재하지 않는 roomId는 `404 Not Found`, `success=false`, `data=null`, `error.code=ROOM_NOT_FOUND`를 반환한다.
