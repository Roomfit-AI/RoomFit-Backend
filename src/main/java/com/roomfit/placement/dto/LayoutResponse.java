package com.roomfit.placement.dto;

import com.roomfit.placement.Layout;
import com.roomfit.placement.PlacementResult;
import com.roomfit.placement.RecommendationStatus;
import com.roomfit.placement.ScoreSummary;
import com.roomfit.placement.ValidationResult;
import com.roomfit.room.Furniture;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.time.LocalDateTime;

@Schema(description = "추천/수정 저장 배치 응답. 프론트는 recommendedFurniture를 Three.js 가구 렌더링에 사용합니다.")
public class LayoutResponse {

    @Schema(description = "배치 ID", example = "1")
    private final Long layoutId;
    @Schema(description = "배치가 속한 방 ID", example = "1")
    private final Long roomId;
    @Schema(description = "재편집 Draft의 원본 배치 ID. 추천 배치는 null", example = "1", nullable = true)
    private final Long sourceLayoutId;
    @Schema(description = "최종 확정 여부")
    private final boolean confirmed;
    @Schema(description = "확정 시각. 미확정 Draft는 null", nullable = true)
    private final LocalDateTime confirmedAt;
    @Schema(description = "추천/저장 상태", example = "SUCCESS")
    private final RecommendationStatus status;
    @Schema(description = "렌더링할 최종 추천 가구 배열. productId/variantId/styleTags는 선택 제품 기반 추천일 때 포함됩니다.")
    private final List<Furniture> recommendedFurniture;
    @Schema(description = "추천 점수 요약")
    private final ScoreSummary scoreSummary;
    @Schema(description = "최종 가구 배치 검증 결과")
    private final ValidationResult validationResult;

    private LayoutResponse(Layout layout, RecommendationStatus status,
                           ScoreSummary scoreSummary, ValidationResult validationResult) {
        this.layoutId = layout.getId();
        this.roomId = layout.getRoomId();
        this.sourceLayoutId = layout.getSourceLayoutId();
        this.confirmed = layout.isConfirmed();
        this.confirmedAt = layout.getConfirmedAt();
        this.status = status;
        this.recommendedFurniture = layout.getFurniture();
        this.scoreSummary = scoreSummary;
        this.validationResult = validationResult;
    }

    public static LayoutResponse ofRecommendation(Layout layout, PlacementResult placementResult,
                                                   ValidationResult validationResult) {
        return new LayoutResponse(layout, placementResult.getStatus(),
                placementResult.getScoreSummary(), validationResult);
    }

    public static LayoutResponse ofUpdate(Layout layout, RecommendationStatus status,
                                           ScoreSummary scoreSummary, ValidationResult validationResult) {
        return new LayoutResponse(layout, status, scoreSummary, validationResult);
    }

    public static LayoutResponse ofSnapshot(Layout layout, ScoreSummary scoreSummary,
                                            ValidationResult validationResult) {
        return new LayoutResponse(layout, RecommendationStatus.SUCCESS, scoreSummary, validationResult);
    }

    public Long getLayoutId() {
        return layoutId;
    }

    public Long getRoomId() {
        return roomId;
    }

    public Long getSourceLayoutId() {
        return sourceLayoutId;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
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
