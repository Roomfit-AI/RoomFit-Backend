RoomFit Frontend API 명세서 v0.1
Base URL
http://localhost:8080
공통 응답 형식

모든 API는 기본적으로 아래 형식을 사용합니다.

{
  "success": true,
  "data": {},
  "error": null
}

실패 시 예시:

{
  "success": false,
  "data": null,
  "error": {
    "code": "ROOM_NOT_FOUND",
    "message": "존재하지 않는 방입니다."
  }
}
1. 샘플 방 목록 조회
GET /api/rooms/samples
용도

데모용 샘플 방 목록 조회

Response
{
  "success": true,
  "data": [
    {
      "roomId": 1,
      "name": "Sample Room",
      "room": {
        "width": 3.2,
        "depth": 4.5,
        "height": 2.4,
        "unit": "meter"
      },
      "openings": [],
      "furniture": [],
      "source": "SAMPLE",
      "createdAt": "2026-07-09T02:37:36.798185"
    }
  ],
  "error": null
}
2. 특정 방 조회
GET /api/rooms/{roomId}
Example
GET /api/rooms/1
Response
{
  "success": true,
  "data": {
    "roomId": 1,
    "name": "Sample Room",
    "room": {
      "width": 3.2,
      "depth": 4.5,
      "height": 2.4,
      "unit": "meter"
    },
    "openings": [
      {
        "id": "door-1",
        "type": "door",
        "wall": "south",
        "offset": 0.7,
        "width": 0.8,
        "height": 2.1,
        "sillHeight": null
      }
    ],
    "furniture": [
      {
        "id": "bed-1",
        "type": "bed",
        "label": "침대",
        "width": 1.1,
        "depth": 2.0,
        "height": 0.45,
        "position": {
          "x": 0.8,
          "z": 1.4
        },
        "rotation": 0,
        "status": "EXISTING",
        "productId": null,
        "styleTags": []
      }
    ],
    "source": "SAMPLE",
    "createdAt": "2026-07-09T02:37:36.798185"
  },
  "error": null
}
3. RoomPlan 방 업로드
POST /api/rooms/upload
용도

RoomPlan iOS App에서 생성한 방 JSON을 백엔드에 저장하고 roomId를 발급받습니다.

Request
{
  "name": "RoomPlan Scan Room",
  "room": {
    "width": 3.2,
    "depth": 4.5,
    "height": 2.4,
    "unit": "meter"
  },
  "openings": [
    {
      "id": "door-1",
      "type": "door",
      "wall": "south",
      "offset": 0.7,
      "width": 0.8,
      "height": 2.1,
      "sillHeight": null
    }
  ],
  "furniture": [
    {
      "id": "bed-1",
      "type": "bed",
      "label": "침대",
      "width": 1.1,
      "depth": 2.0,
      "height": 0.45,
      "position": {
        "x": 0.8,
        "z": 1.4
      },
      "rotation": 89,
      "status": "EXISTING"
    }
  ]
}
Response
{
  "success": true,
  "data": {
    "roomId": 3,
    "name": "RoomPlan Scan Room",
    "room": {
      "width": 3.2,
      "depth": 4.5,
      "height": 2.4,
      "unit": "meter"
    },
    "openings": [],
    "furniture": [],
    "source": "ROOMPLAN",
    "createdAt": "2026-07-09T02:13:15.411289"
  },
  "error": null
}
4. 기존 가구 상태 수정
PUT /api/rooms/{roomId}/furniture
용도

기존 가구 삭제, 유지, 위치 변경 상태를 저장합니다.
예를 들어 기존 책상을 삭제하고 새 책상을 추천받고 싶을 때 사용합니다.

Request
{
  "furnitureUpdates": [
    {
      "id": "bed-1",
      "status": "EXISTING",
      "position": {
        "x": 0.8,
        "z": 1.4
      },
      "rotation": 0
    },
    {
      "id": "desk-1",
      "status": "DELETED",
      "position": {
        "x": 2.7,
        "z": 0.4
      },
      "rotation": 90
    }
  ]
}
Response
{
  "success": true,
  "data": {
    "roomId": 1,
    "furniture": [
      {
        "id": "desk-1",
        "status": "DELETED"
      }
    ]
  },
  "error": null
}
5. 제품 목록 조회
GET /api/products/mock
용도

프론트에서 제품 선택 카드 UI를 렌더링합니다.

Response
{
  "success": true,
  "data": [
    {
      "productId": "desk-01",
      "type": "desk",
      "name": "화이트 미니멀 책상",
      "brand": "RoomFit Mock",
      "width": 1.2,
      "depth": 0.6,
      "height": 0.72,
      "price": 89000,
      "styleTags": ["minimal", "white_tone", "study"],
      "imageUrl": "/images/products/desk-white.png",
      "requiredClearance": {
        "front": 0.6,
        "side": 0.2
      }
    }
  ],
  "error": null
}
6. Agent Context 생성
POST /api/agent/context
용도

사용자의 생활 목표, 디자인 스타일, 선택 제품 정보를 백엔드에 전달합니다.

Request
{
  "roomId": 1,
  "lifestyleGoal": "STUDY_FOCUSED",
  "designStyle": ["MINIMAL", "WHITE_TONE"],
  "requiredItems": ["bed", "desk", "chair"],
  "optionalItems": ["storage", "rug", "lamp"],
  "selectedImageIds": [1, 3],
  "selectedProductIds": ["desk-01", "chair-01", "lamp-01"]
}
Response
{
  "success": true,
  "data": {
    "contextId": 1,
    "roomId": 1,
    "lifestyleGoal": "STUDY_FOCUSED",
    "designStyle": ["MINIMAL", "WHITE_TONE"],
    "requiredItems": ["bed", "desk", "chair"],
    "optionalItems": ["storage", "rug", "lamp"],
    "selectedImageIds": [1, 3],
    "selectedProductIds": ["desk-01", "chair-01", "lamp-01"],
    "styleTags": ["minimal", "white_tone", "open_space", "study", "desk_zone", "cozy"],
    "selectedProducts": [
      {
        "productId": "desk-01",
        "type": "desk",
        "name": "화이트 미니멀 책상",
        "width": 1.2,
        "depth": 0.6,
        "height": 0.72,
        "styleTags": ["minimal", "white_tone", "study"],
        "requiredClearance": {
          "front": 0.6,
          "side": 0.2
        }
      }
    ],
    "createdAt": "2026-07-09T02:39:38.890864"
  },
  "error": null
}
7. 배치 추천 생성
POST /api/layouts/recommend
용도

Agent Context를 기반으로 최종 가구 배치를 추천합니다.

Request
{
  "contextId": 1
}
Response
{
  "success": true,
  "data": {
    "layoutId": 1,
    "status": "SUCCESS",
    "recommendedFurniture": [
      {
        "id": "desk-rec-1",
        "type": "desk",
        "label": "화이트 미니멀 책상",
        "width": 1.2,
        "depth": 0.6,
        "height": 0.72,
        "position": {
          "x": 2.3,
          "z": 1.0
        },
        "rotation": 0,
        "status": "RECOMMENDED",
        "productId": "desk-01",
        "styleTags": ["minimal", "white_tone", "study"]
      }
    ],
    "scoreSummary": {
      "collisionScore": 100,
      "boundaryScore": 100,
      "doorWindowScore": 100,
      "pathScore": 100,
      "goalScore": 95,
      "styleScore": 95,
      "totalScore": 590
    },
    "validationResult": {
      "collisionFree": true,
      "boundaryValid": true,
      "doorClearance": true,
      "windowClearance": true,
      "pathSecured": true,
      "validationItems": [
        {
          "type": "collision",
          "passed": true,
          "message": "가구 충돌 없음"
        }
      ],
      "warnings": []
    }
  },
  "error": null
}
8. 배치 검증
POST /api/layouts/validate
용도

드래그 중 실시간 검증에 사용합니다.
저장은 하지 않고 검증 결과만 반환합니다.

Request
{
  "layoutId": 1,
  "furniture": [
    {
      "id": "desk-rec-1",
      "type": "desk",
      "label": "화이트 미니멀 책상",
      "width": 1.2,
      "depth": 0.6,
      "height": 0.72,
      "position": {
        "x": 2.3,
        "z": 1.0
      },
      "rotation": 0,
      "status": "USER_MODIFIED",
      "productId": "desk-01",
      "styleTags": ["minimal", "white_tone", "study"]
    }
  ]
}
Response
{
  "success": true,
  "data": {
    "collisionFree": true,
    "boundaryValid": true,
    "doorClearance": true,
    "windowClearance": true,
    "pathSecured": true,
    "validationItems": [
      {
        "type": "collision",
        "passed": true,
        "message": "가구 충돌 없음"
      },
      {
        "type": "boundary",
        "passed": true,
        "message": "방 범위 내 배치"
      },
      {
        "type": "door_clearance",
        "passed": true,
        "message": "문 앞 공간 확보"
      },
      {
        "type": "window_clearance",
        "passed": true,
        "message": "창문 앞 공간 확보"
      },
      {
        "type": "path",
        "passed": true,
        "message": "이동 동선 확보"
      }
    ],
    "warnings": []
  },
  "error": null
}
9. 배치 수정 저장
PUT /api/layouts/{layoutId}
용도

사용자가 드래그로 수정한 최종 배치를 저장하고, 점수와 검증 결과를 다시 계산합니다.

Request
{
  "furniture": [
    {
      "id": "desk-rec-1",
      "type": "desk",
      "label": "화이트 미니멀 책상",
      "width": 1.2,
      "depth": 0.6,
      "height": 0.72,
      "position": {
        "x": 2.3,
        "z": 1.0
      },
      "rotation": 0,
      "status": "USER_MODIFIED",
      "productId": "desk-01",
      "styleTags": ["minimal", "white_tone", "study"]
    }
  ]
}
Response
{
  "success": true,
  "data": {
    "layoutId": 1,
    "status": "SUCCESS",
    "recommendedFurniture": [],
    "scoreSummary": {
      "collisionScore": 100,
      "boundaryScore": 100,
      "doorWindowScore": 100,
      "pathScore": 100,
      "goalScore": 95,
      "styleScore": 95,
      "totalScore": 590
    },
    "validationResult": {
      "collisionFree": true,
      "boundaryValid": true,
      "doorClearance": true,
      "windowClearance": true,
      "pathSecured": true,
      "validationItems": [],
      "warnings": []
    }
  },
  "error": null
}
10. 최종 배치 확정
POST /api/layouts/{layoutId}/confirm
용도

사용자가 최종 추천 배치를 확정할 때 사용합니다.

Request

현재 MVP 기준 별도 body가 없거나 최소 body로 처리 가능.

{}
Response
{
  "success": true,
  "data": {
    "layoutId": 1,
    "confirmed": true
  },
  "error": null
}

이 API는 실제 응답을 한 번 더 확인한 뒤 프론트에 넘기는 게 좋아.

11. Enum 명세
FurnitureStatus
EXISTING
DELETED
RECOMMENDED
USER_MODIFIED
LifestyleGoal
STUDY_FOCUSED
RELAX_FOCUSED
STORAGE_FOCUSED
WFH_FOCUSED
DesignStyle
MINIMAL
NATURAL
WHITE_TONE
WOOD_TONE
COZY
MODERN
FurnitureType
bed
desk
chair
storage
rug
lamp
RecommendationStatus
SUCCESS
FALLBACK
RoomSource
SAMPLE
ROOMPLAN
12. 프론트 구현 핵심 주의사항
1. 모든 길이 단위는 meter입니다.
2. 좌표는 x-z 평면입니다.
3. position은 가구의 중심 좌표입니다.
4. rotation은 degree입니다.
5. recommendedFurniture에는 기존 가구와 추천 가구가 함께 포함됩니다.
6. DELETED 상태 가구는 렌더링하지 않는 것이 자연스럽습니다.
7. POST /api/layouts/validate는 저장하지 않습니다.
8. PUT /api/layouts/{layoutId}는 저장 + 검증 + scoreSummary 재계산입니다.
9. RoomPlan 업로드 방도 GET /api/rooms/{roomId} 응답 구조로 동일하게 렌더링하면 됩니다.