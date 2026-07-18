package com.roomfit.placement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "active Draft에 사용자가 선택한 추가 희망 가구만 배치하는 요청")
public class DraftFurnitureAdditionRequest {

    @Schema(description = "Preference와 Add Furniture 선택을 반영한 Agent Context ID", example = "1")
    private Long contextId;

    protected DraftFurnitureAdditionRequest() {
        // JSON 역직렬화용
    }

    public Long getContextId() {
        return contextId;
    }
}
