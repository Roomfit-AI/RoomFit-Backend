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

에러 처리 원칙

프론트는 error.message 문자열보다 error.code를 기준으로 분기하는 것을 권장합니다.
실제 에러 코드는 Swagger 또는 응답 body의 error.code를 기준으로 처리합니다.
대표 에러 코드 예시는 아래와 같습니다.

- INVALID_REQUEST_BODY
- CONTEXT_NOT_FOUND
- FURNITURE_NOT_FOUND
- STYLE_IMAGE_NOT_FOUND
- INVALID_FURNITURE_STATUS
- INVALID_LIFESTYLE_GOAL
- INVALID_DESIGN_STYLE
- INVALID_FURNITURE_TYPE
- INVALID_ROOM_DIMENSION
- INVALID_OPENING_DATA
- REQUIRED_ITEM_EMPTY
- STYLE_IMAGE_EMPTY
- INVALID_ROTATION
- INVALID_FURNITURE_POSITION
- FURNITURE_ARRAY_MISMATCH
- PRODUCT_NOT_FOUND
- ROOM_NOT_FOUND
- LAYOUT_NOT_FOUND
- ALREADY_CONFIRMED
- UNSUPPORTED_FEEDBACK_INTENT
- RECOMMENDATION_FAILED

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
응답의 name은 백엔드가 부여하는 방 표시 이름입니다.
프론트는 name을 고정 문자열로 가정하지 말고 응답값을 그대로 표시해야 합니다.
스캔 오차가 있으면 백엔드는 업로드 단계에서만 기존 strict validator를 사용해
가구를 안전한 위치로 재배치하거나 제외할 수 있습니다. 이 경우 additive
`importStatus`와 `importWarnings`를 반환합니다. 기존 앱은 이 필드를 무시해도 됩니다.
UUID client isolation을 사용하는 앱은 `X-RoomFit-Client-Id` header를 함께 보내야 하며,
현재 RoomPlan은 header가 없으면 legacy scope로 업로드됩니다.

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
4. 기존 가구 상태 변경
PUT /api/rooms/{roomId}/furniture
용도

기존 방 가구를 DELETED 등으로 상태 변경합니다.
예를 들어 기존 책상을 삭제하고 새 책상을 추천받고 싶을 때 사용합니다.
각 furniture update item에서 id와 status는 필수입니다.
요청 필드명은 layout validate/update의 furniture가 아니라 furnitureUpdates입니다.
이 API는 기존 가구 상태 변경 전용이며, 가구 위치 변경은 layout validate/update 흐름에서 처리합니다.

Request
{
  "furnitureUpdates": [
    {
      "id": "desk-1",
      "status": "DELETED"
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
5. 스타일 이미지 목록 조회
GET /api/styles/images
용도

프론트에서 선호 스타일 이미지 선택 UI를 렌더링합니다.
스타일 이미지 응답은 Product API의 styleTags가 아니라 tags 필드를 사용합니다.

Response
{
  "success": true,
  "data": [
    {
      "imageId": 1,
      "title": "화이트톤 미니멀 원룸",
      "imageUrl": "/images/styles/minimal-white-1.jpg",
      "tags": ["minimal", "white_tone", "open_space"]
    }
  ],
  "error": null
}
6. 제품 목록 조회
GET /api/products/mock
용도

프론트에서 제품 선택 카드 UI를 렌더링합니다.

Response
{
  "success": true,
  "data": [
    {
      "productId": "desk-01",
      "variantId": null,
      "type": "desk",
      "name": "화이트 미니멀 책상",
      "brand": "RoomFit Mock",
      "width": 1.2,
      "depth": 0.6,
      "height": 0.72,
      "price": 89000,
      "styleTags": ["minimal", "white_tone", "study"],
      "imageUrl": "/images/products/desk-white.png",
      "purchaseUrl": "https://www.ikea.com/kr/ko/p/micke-desk-white-80354281/",
      "requiredClearance": {
        "front": 0.6,
        "side": 0.2
      }
    },
    {
      "productId": "desk-compact-01",
      "variantId": "desk-compact",
      "type": "desk",
      "name": "컴팩트 책상",
      "brand": null,
      "width": 1.2,
      "depth": 0.6,
      "height": 0.73,
      "price": null,
      "styleTags": ["minimal", "classic"],
      "imageUrl": null,
      "purchaseUrl": "https://www.ikea.com/kr/ko/p/lagkapten-adils-desk-white-s09416759/",
      "requiredClearance": {
        "front": 0.6,
        "side": 0.2
      }
    }
  ],
  "error": null
}

`brand`, `price`, `imageUrl`, `purchaseUrl`은 `null`일 수 있습니다. `price=null`을 0원으로 표시하지 말고,
`brand=null`이면 브랜드 영역을 생략하며, `imageUrl=null`이면 프론트 fallback 이미지를 사용해야 합니다.
`purchaseUrl`은 정확히 동일한 상품을 보장하지 않는 "유사 제품 보기" 링크입니다.

Product의 `width`, `depth`, `height`는 RoomFit 추천·경계·충돌 계산에 사용하는 Furniture Variant 치수입니다.
판매 페이지에 표시된 실제 상품 치수와 다를 수 있으며, 공간 계산에는 API 응답 치수를 사용해야 합니다.
가구 유형별 Variant 수와 Product 전체 개수는 가변적이므로 프론트는 배열 길이나 고정 index에 의존하면 안 됩니다.
`variantId`는 JSON 기반 Furniture Variant Registry의 key입니다. `null`이거나 프론트 Registry에 등록되지 않은 값이면 기존 가구 Renderer를 사용해야 합니다.
7. Agent Context 생성
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
        "variantId": null,
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

주요 에러

- 400 INVALID_LIFESTYLE_GOAL: lifestyleGoal 값이 올바르지 않습니다.
- 400 INVALID_DESIGN_STYLE: designStyle 값이 올바르지 않습니다.
- 400 INVALID_FURNITURE_TYPE: requiredItems 또는 optionalItems의 가구 타입 값이 올바르지 않습니다.
- 400 REQUIRED_ITEM_EMPTY: requiredItems가 비어 있습니다.
- 400 STYLE_IMAGE_EMPTY: selectedImageIds가 비어 있습니다.
- 400 PRODUCT_NOT_FOUND: selectedProductIds에 존재하지 않는 Mock Product ID가 포함되어 있습니다.
- 404 ROOM_NOT_FOUND: roomId가 존재하지 않습니다.
- 404 STYLE_IMAGE_NOT_FOUND: selectedImageIds에 존재하지 않는 스타일 이미지 ID가 포함되어 있습니다.

8. 배치 추천 생성
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
    "recommendationStatus": "SUCCESS",
    "requestedFurnitureCount": 1,
    "placedFurnitureCount": 1,
    "unplacedFurniture": [],
    "warningCode": null,
    "message": "선택한 1개 가구를 모두 배치했습니다.",
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
        "variantId": null,
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

추천 가능성 결과

- `recommendationStatus`는 기존 `status`와 별개이며 `SUCCESS`, `PARTIAL_SUCCESS`, `FAILED` 중 하나입니다.
- 공간 부족 같은 정상 배치 실패는 HTTP 500이 아니라 201 응답의 `unplacedFurniture`로 반환됩니다.
- `FAILED`이면 `layoutId`는 `null`이고 새 Layout snapshot을 저장하지 않습니다.
- `unplacedFurniture`는 요청 순서를 보존하며 `reasonCode`, 선택된 `productId`/`variantId`(있을 경우), 결정론적 메시지를 포함합니다.
9. 배치 검증
POST /api/layouts/validate
용도

드래그 중 실시간 검증에 사용합니다.
저장은 하지 않고 검증 결과만 반환합니다.
POST /api/layouts/validate는 현재 layout의 전체 furniture id 목록을 포함한 배열을 받아 검증합니다.
여기서 전체 가구 배열이란 모든 furniture id를 포함해야 한다는 뜻이지, 각 가구의 모든 메타데이터 필드를 보내야 한다는 뜻이 아닙니다.
각 item은 full furniture object가 아니라 id, position, rotation, status 중심의 compact update item입니다.
width, depth, height, productId, variantId, styleTags 등은 백엔드 추천 결과가 가진 메타데이터이며 요청에서 다시 전달하지 않습니다.
일부 furniture id만 보내는 partial furniture array는 FURNITURE_ARRAY_MISMATCH를 발생시킬 수 있습니다.

Request
{
  "layoutId": 1,
  "furniture": [
    {
      "id": "bed-1",
      "position": {
        "x": 0.8,
        "z": 1.4
      },
      "rotation": 0,
      "status": "EXISTING"
    },
    {
      "id": "desk-rec-1",
      "position": {
        "x": 2.3,
        "z": 1.0
      },
      "rotation": 0,
      "status": "USER_MODIFIED"
    },
    {
      "id": "chair-rec-1",
      "position": {
        "x": 2.3,
        "z": 1.8
      },
      "rotation": 0,
      "status": "USER_MODIFIED"
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

주요 에러

- 400 FURNITURE_ARRAY_MISMATCH: 현재 layout의 전체 furniture id 목록과 요청 furniture 배열이 일치하지 않습니다.
- 400 FURNITURE_NOT_FOUND: 요청 furniture 배열에 현재 layout에 없는 id가 포함되어 있습니다.
- 400 INVALID_ROTATION: rotation 값이 올바르지 않습니다.
- 400 INVALID_FURNITURE_POSITION: position 값이 방 범위를 벗어났거나 누락되었습니다.
- 400 INVALID_FURNITURE_STATUS: status 값이 올바르지 않습니다.
- 404 LAYOUT_NOT_FOUND: layoutId가 존재하지 않습니다.

10. 배치 수정 저장
PUT /api/layouts/{layoutId}
용도

사용자가 드래그로 수정한 최종 배치를 저장하고, 점수와 검증 결과를 다시 계산합니다.
PUT /api/layouts/{layoutId}도 현재 layout의 전체 furniture id 목록을 포함한 compact update item 배열을 받습니다.
각 item은 id, position, rotation, status 중심이며 type, label, width, depth, height, productId, variantId, styleTags는 요청 필드가 아닙니다.
일부 furniture id만 보내는 partial furniture array는 FURNITURE_ARRAY_MISMATCH를 발생시킬 수 있습니다.
이미 확정된 layout은 수정할 수 없으며 409 ALREADY_CONFIRMED가 반환됩니다.

Request
{
  "furniture": [
    {
      "id": "bed-1",
      "position": {
        "x": 0.8,
        "z": 1.4
      },
      "rotation": 0,
      "status": "EXISTING"
    },
    {
      "id": "desk-rec-1",
      "position": {
        "x": 2.3,
        "z": 1.0
      },
      "rotation": 0,
      "status": "USER_MODIFIED"
    },
    {
      "id": "chair-rec-1",
      "position": {
        "x": 2.3,
        "z": 1.8
      },
      "rotation": 0,
      "status": "USER_MODIFIED"
    }
  ]
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
        "status": "USER_MODIFIED",
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
      "validationItems": [],
      "warnings": []
    }
  },
  "error": null
}

주요 에러

- 400 FURNITURE_ARRAY_MISMATCH: 현재 layout의 전체 furniture id 목록과 요청 furniture 배열이 일치하지 않습니다.
- 400 FURNITURE_NOT_FOUND: 요청 furniture 배열에 현재 layout에 없는 id가 포함되어 있습니다.
- 400 INVALID_ROTATION: rotation 값이 올바르지 않습니다.
- 400 INVALID_FURNITURE_POSITION: position 값이 방 범위를 벗어났거나 누락되었습니다.
- 400 INVALID_FURNITURE_STATUS: status 값이 올바르지 않습니다.
- 404 LAYOUT_NOT_FOUND: layoutId가 존재하지 않습니다.
- 409 ALREADY_CONFIRMED: 이미 확정된 layout은 수정할 수 없습니다.

11. 사용자 피드백 기반 재추천
POST /api/layouts/feedback
용도

사용자의 자연어 피드백을 바탕으로 기존 배치를 조정하거나 재추천합니다.
Plan v2 지원 문장은 단일 또는 지원되는 복합 operation으로 해석됩니다. 복합 요청은 모두 성공할 때만 새 layout이 저장됩니다.

Request
{
  "layoutId": 1,
  "feedback": "의자를 삭제하고 협탁을 추가해줘",
  "selectedFurnitureId": "chair-1"
}

`selectedFurnitureId`는 `AMBIGUOUS_TARGET` 후보를 사용자가 선택한 후에만 보냅니다. reference ambiguity나 product failure에는 보내지 않습니다. clarification/failure 응답은 source `layoutId`를 유지하며 새 layout을 만들지 않습니다.
Response
{
  "success": true,
  "data": {
    "layoutId": 2,
    "status": "SUCCESS",
    "feedbackStatus": "SUCCESS",
    "operationResults": [
      {
        "operationId": "op-1",
        "operationType": "REPLACE_PRODUCT",
        "status": "APPLIED",
        "reasonCode": null,
        "message": "가구 제품을 교체했습니다.",
        "targetFurnitureId": "desk-1",
        "resultFurnitureId": "desk-1",
        "productId": "desk-storage-01",
        "variantId": "desk-storage"
      }
    ],
    "recommendedFurniture": [
      {
        "id": "desk-rec-1",
        "type": "desk",
        "label": "화이트 미니멀 책상",
        "width": 1.4,
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
      "validationItems": [],
      "warnings": []
    },
    "interpretedIntent": {
      "deskMinWidth": 1.4
    }
  },
  "error": null
}

주요 에러

- 400 UNSUPPORTED_FEEDBACK_INTENT: 지원하지 않는 피드백 문장입니다.
- 404 LAYOUT_NOT_FOUND: layoutId가 존재하지 않습니다.
- 404 CONTEXT_NOT_FOUND: layout에 연결된 contextId가 존재하지 않습니다.
- 404 ROOM_NOT_FOUND: layout에 연결된 roomId가 존재하지 않습니다.

Feedback 실행 결과

- `feedbackStatus`는 기존 `status`와 별개인 전체 실행 결과입니다. `SUCCESS`, `PARTIAL_SUCCESS`, `FAILED`, `NEEDS_CLARIFICATION` 중 하나입니다.
- `operationResults`는 Plan 순서대로 반환됩니다. 의존 작업이 적용되지 않으면 하위 작업은 `SKIPPED_DEPENDENCY`와 `DEPENDENCY_NOT_APPLIED`를 반환합니다.
- 하나 이상의 작업만 성공한 경우 성공한 변경만 새 snapshot에 저장되고 `feedbackStatus`는 `PARTIAL_SUCCESS`입니다. 모두 실패하거나 재질문이 필요하면 입력 `layoutId`를 그대로 반환합니다.
- 모호한 대상은 `clarification`(그리고 복수인 경우 `clarifications`)에 후보 최대 10개와 `requiredField`를 반환합니다. 클라이언트는 첫 후보를 임의 선택하지 않아야 합니다.

12. 최종 배치 확정
POST /api/layouts/{layoutId}/confirm
용도

사용자가 최종 추천 배치를 확정할 때 사용합니다.
이미 확정된 layout은 다시 확정할 수 없으며 409 ALREADY_CONFIRMED가 반환됩니다.

Request

현재 MVP 기준 별도 body가 없거나 최소 body로 처리 가능.

{}
Response
{
  "success": true,
  "data": {
    "layoutId": 1,
    "confirmed": true,
    "confirmedAt": "2026-07-09T02:55:10.123456"
  },
  "error": null
}

이 API는 실제 응답을 한 번 더 확인한 뒤 프론트에 넘기는 게 좋아.

주요 에러

- 404 LAYOUT_NOT_FOUND: layoutId가 존재하지 않습니다.
- 409 ALREADY_CONFIRMED: 이미 확정된 layout은 다시 확정할 수 없습니다.

13. Enum 명세
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
14. 프론트 구현 핵심 주의사항
1. 모든 길이 단위는 meter입니다.
2. 좌표는 x-z 평면입니다.
3. position은 가구의 중심 좌표입니다.
4. rotation은 degree입니다.
5. recommendedFurniture에는 기존 가구와 추천 가구가 함께 포함됩니다.
6. DELETED 상태 가구는 렌더링하지 않는 것이 자연스럽습니다.
7. POST /api/layouts/validate는 저장하지 않습니다.
8. PUT /api/layouts/{layoutId}는 저장 + 검증 + scoreSummary 재계산입니다.
9. RoomPlan 업로드 방도 GET /api/rooms/{roomId} 응답 구조로 동일하게 렌더링하면 됩니다.
10. validate/update 요청의 furniture item은 compact update item입니다. 추천 결과의 width/depth/height/productId/variantId/styleTags 메타데이터를 다시 보내지 않습니다.
11. recommendedFurniture의 variantId가 null이거나 프론트 Registry에 없으면 기존 가구 Renderer로 fallback합니다.
12. 에러 분기는 error.message보다 error.code를 기준으로 처리하는 것을 권장합니다.
