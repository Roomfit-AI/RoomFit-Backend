package com.roomfit.placement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "사용자 자연어 피드백 기반 재추천 요청")
public class FeedbackRequest {

    @Schema(description = "피드백을 적용할 기존 배치 ID", example = "1")
    private Long layoutId;
    @Schema(description = "지원 피드백 문장", example = "책상 더 크게", allowableValues = {"책상 더 크게", "수납 늘려줘", "방이 넓어 보이게"})
    private String feedback;
    @Schema(description = "사용자가 UI에서 선택한 target ambiguity 해소용 내부 식별자. 활성 가구 및 요청 canonical type 검증을 통과해야 하며 provider의 다른 target이나 reference/product ambiguity를 대체하지 않습니다.", nullable = true)
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
