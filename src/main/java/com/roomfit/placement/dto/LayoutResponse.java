package com.roomfit.placement.dto;

import com.roomfit.placement.Layout;
import com.roomfit.placement.PlacementResult;
import com.roomfit.placement.RecommendationStatus;
import com.roomfit.placement.ScoreSummary;
import com.roomfit.placement.ValidationResult;
import com.roomfit.room.Furniture;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "추천/수정 저장 배치 응답. 프론트는 recommendedFurniture를 Three.js 가구 렌더링에 사용합니다.")
public class LayoutResponse {

    @Schema(description = "배치 ID", example = "1")
    private final Long layoutId;
    @Schema(description = "추천/저장 상태", example = "SUCCESS")
    private final RecommendationStatus status;
    @Schema(description = "렌더링할 최종 추천 가구 배열. productId/styleTags는 선택 제품 기반 추천일 때 포함됩니다.")
    private final List<Furniture> recommendedFurniture;
    @Schema(description = "추천 점수 요약")
    private final ScoreSummary scoreSummary;
    @Schema(description = "최종 가구 배치 검증 결과")
    private final ValidationResult validationResult;

    private LayoutResponse(Long layoutId, RecommendationStatus status,
                            List<Furniture> recommendedFurniture, ScoreSummary scoreSummary,
                            ValidationResult validationResult) {
        this.layoutId = layoutId;
        this.status = status;
        this.recommendedFurniture = recommendedFurniture;
        this.scoreSummary = scoreSummary;
        this.validationResult = validationResult;
    }

    public static LayoutResponse ofRecommendation(Layout layout, PlacementResult placementResult,
                                                   ValidationResult validationResult) {
        return new LayoutResponse(layout.getId(), placementResult.getStatus(),
                layout.getFurniture(), placementResult.getScoreSummary(), validationResult);
    }

    public static LayoutResponse ofUpdate(Layout layout, RecommendationStatus status,
                                           ScoreSummary scoreSummary, ValidationResult validationResult) {
        return new LayoutResponse(layout.getId(), status, layout.getFurniture(), scoreSummary, validationResult);
    }

    public Long getLayoutId() {
        return layoutId;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public List<Furniture> getRecommendedFurniture() {
        return recommendedFurniture;
    }

    public ScoreSummary getScoreSummary() {
        return scoreSummary;
    }

    public ValidationResult getValidationResult() {
        return validationResult;
    }
}
