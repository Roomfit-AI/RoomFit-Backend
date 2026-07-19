package com.roomfit.placement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 자연어 피드백 기반 재추천 요청")
public class FeedbackRequest {

    @Schema(description = "피드백을 적용할 기존 배치 ID", example = "1")
    private Long layoutId;
    @Schema(description = "지원 피드백 문장", example = "책상 더 크게", allowableValues = {"책상 더 크게", "수납 늘려줘", "방이 넓어 보이게"})
    private String feedback;
    @Schema(description = "선택된 가구의 내부 식별자. 모호한 대상을 명시할 때만 사용합니다.", nullable = true)
    private String selectedFurnitureId;

    protected FeedbackRequest() {
        // JSON 역직렬화용
    }

    public Long getLayoutId() {
        return layoutId;
    }

    public String getFeedback() {
        return feedback;
    }

    public String getSelectedFurnitureId() {
        return selectedFurnitureId;
    }
}
