# Room Samples API Design

## 목표

`GET /api/rooms/samples`는 MVP에서 사용 가능한 샘플 Room JSON 목록을 반환한다. 응답 body는 `CommonResponse<T>` 구조를 유지한다.

## 범위

- 포함: `GET /api/rooms/samples`
- 제외: `GET /api/rooms/{roomId}` 보강, `PUT /api/rooms/{roomId}/furniture` 보강

## 응답

명세에 없는 요약 필드를 새로 만들지 않기 위해 기존 `RoomResponse`를 재사용한다.

```json
{
  "success": true,
  "data": [
    {
      "roomId": 1,
      "room": {
        "width": 3.2,
        "depth": 4.5,
        "height": 2.4,
        "unit": "meter"
      },
      "openings": [],
      "furniture": []
    }
  ],
  "error": null
}
```

## Repository

현재 `RoomRepository`는 `findById`만 제공하므로 `findAll()`을 추가한다. in-memory Map 저장소 기준으로 모든 샘플 Room을 id 오름차순으로 반환한다.

## 테스트

- 정상 요청은 `200 OK`, `success=true`, `error=null`을 반환한다.
- `data`는 배열이며, 첫 샘플의 `roomId`, `room.width`, `room.depth`, `room.height`, `room.unit`, `openings`, `furniture`를 포함한다.
