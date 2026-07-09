package com.roomfit.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "기존 방 가구의 상태 변경 요청. 요청 필드명은 furnitureUpdates입니다. 각 항목의 id와 status는 필수입니다. 이 API는 위치 변경을 처리하지 않으며, 가구 위치 변경은 layout validate/update 흐름에서 처리합니다. DELETED 가구는 프론트 렌더링/검증에서 제외하는 것이 자연스럽습니다.")
public class FurnitureUpdateRequest {

    @Schema(description = "상태를 변경할 기존 가구 목록. layout validate/update의 furniture 필드와 다르게 rooms furniture update는 furnitureUpdates 필드를 사용합니다.")
    private List<Item> furnitureUpdates;

    protected FurnitureUpdateRequest() {
        // JSON 역직렬화용
    }

    public List<Item> getFurnitureUpdates() {
        return furnitureUpdates;
    }

    @Schema(description = "기존 가구 상태 변경 항목. id/status는 필수입니다.")
    public static class Item {
        @Schema(description = "기존 가구 ID. 필수입니다.", example = "desk-1")
        private String id;
        @Schema(description = "변경할 상태. 필수입니다.", example = "DELETED", allowableValues = {"EXISTING", "DELETED", "RECOMMENDED", "USER_MODIFIED"})
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
