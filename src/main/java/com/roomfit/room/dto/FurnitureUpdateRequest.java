package com.roomfit.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "기존 방 가구의 상태 변경 요청. DELETED 가구는 프론트 렌더링/검증에서 제외하는 것이 자연스럽습니다.")
public class FurnitureUpdateRequest {

    @Schema(description = "상태를 변경할 기존 가구 목록")
    private List<Item> furnitureUpdates;

    protected FurnitureUpdateRequest() {
        // JSON 역직렬화용
    }

    public List<Item> getFurnitureUpdates() {
        return furnitureUpdates;
    }

    @Schema(description = "기존 가구 상태 변경 항목")
    public static class Item {
        @Schema(description = "기존 가구 ID", example = "desk-1")
        private String id;
        @Schema(description = "변경할 상태", example = "DELETED", allowableValues = {"EXISTING", "DELETED", "RECOMMENDED", "USER_MODIFIED"})
        private String status; // "existing" or "deleted"

        protected Item() {
        }

        public String getId() {
            return id;
        }

        public String getStatus() {
            return status;
        }
    }
}
