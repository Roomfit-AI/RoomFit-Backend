package com.roomfit.agent.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "사용자 목표/스타일/선택 제품 기반 추천 Context 생성 요청")
public class AgentContextRequest {

    @Schema(description = "추천 대상 방 ID", example = "1")
    private Long roomId;
    @Schema(description = "생활 목표", example = "STUDY_FOCUSED", allowableValues = {"STUDY_FOCUSED", "RELAX_FOCUSED", "STORAGE_FOCUSED", "WFH_FOCUSED"})
    private String lifestyleGoal;
    @Schema(description = "디자인 스타일 목록", example = "[\"MINIMAL\", \"WHITE_TONE\"]")
    private List<String> designStyle;
    @Schema(description = "반드시 추천하거나 유지해야 하는 가구 타입 목록", example = "[\"bed\", \"desk\", \"chair\"]")
    private List<String> requiredItems;
    @Schema(description = "공간이 허용되면 추가할 선택 가구 타입 목록", example = "[\"storage\", \"rug\", \"lamp\"]")
    private List<String> optionalItems;
    @Schema(description = "선택한 스타일 이미지 ID 목록", example = "[1, 3]")
    private List<Long> selectedImageIds;
    @Schema(description = "선택한 Mock Product ID 목록. 추천 가구 크기/productId/variantId/styleTags에 반영됩니다.", example = "[\"desk-01\", \"chair-01\", \"lamp-01\"]")
    private List<String> selectedProductIds;

    protected AgentContextRequest() {
        // JSON 역직렬화용
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getLifestyleGoal() {
        return lifestyleGoal;
    }

    public List<String> getDesignStyle() {
        return designStyle;
    }

    public List<String> getRequiredItems() {
        return requiredItems;
    }

    public List<String> getOptionalItems() {
        return optionalItems;
    }

    public List<Long> getSelectedImageIds() {
        return selectedImageIds;
    }

    public List<String> getSelectedProductIds() {
        return selectedProductIds;
    }
}
