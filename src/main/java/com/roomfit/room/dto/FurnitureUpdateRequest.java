package com.roomfit.room.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "기존 방 가구의 상태 변경 요청. 각 항목의 id와 status는 필수입니다. status-only 요청이 가능하며 position/rotation을 생략하면 기존 위치와 회전값을 유지합니다. 위치를 실제로 변경하는 요청에서는 position과 rotation을 함께 전달해야 합니다. DELETED 가구는 프론트 렌더링/검증에서 제외하는 것이 자연스럽습니다.")
public class FurnitureUpdateRequest {

    @Schema(description = "상태를 변경할 기존 가구 목록. status만 바꾸는 경우 id/status만 전달합니다.")
    private List<Item> furnitureUpdates;

    protected FurnitureUpdateRequest() {
        // JSON 역직렬화용
    }

    public List<Item> getFurnitureUpdates() {
        return furnitureUpdates;
    }

    @Schema(description = "기존 가구 상태 변경 항목. id/status는 필수이며 position/rotation은 선택값입니다.")
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
