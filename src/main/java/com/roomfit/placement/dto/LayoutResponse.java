package com.roomfit.placement.dto;

import com.roomfit.placement.Layout;
import com.roomfit.placement.PlacementResult;
import com.roomfit.placement.RecommendationStatus;
import com.roomfit.placement.RecommendationExecutionStatus;
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
    @Schema(description = "요청 가구별 실제 배치 결과. 기존 status와 별개의 additive 필드입니다.")
    private final RecommendationExecutionStatus recommendationStatus;
    private final int requestedFurnitureCount;
    private final int placedFurnitureCount;
    private final List<com.roomfit.placement.UnplacedFurniture> unplacedFurniture;
    private final String warningCode;
    private final String message;

    private LayoutResponse(Layout layout, RecommendationStatus status,
                           ScoreSummary scoreSummary, ValidationResult validationResult,
                           PlacementResult placementResult) {
        this.layoutId = layout == null ? null : layout.getId();
        this.roomId = layout == null ? null : layout.getRoomId();
        this.sourceLayoutId = layout == null ? null : layout.getSourceLayoutId();
        this.confirmed = layout != null && layout.isConfirmed();
        this.confirmedAt = layout == null ? null : layout.getConfirmedAt();
        this.status = status;
        this.recommendedFurniture = layout == null ? List.of() : layout.getFurniture();
        this.scoreSummary = scoreSummary;
        this.validationResult = validationResult;
        this.recommendationStatus = placementResult == null ? RecommendationExecutionStatus.SUCCESS
                : placementResult.getRecommendationStatus();
        this.requestedFurnitureCount = placementResult == null ? 0 : placementResult.getRequestedFurnitureCount();
        this.placedFurnitureCount = placementResult == null ? 0 : placementResult.getPlacedFurnitureCount();
        this.unplacedFurniture = placementResult == null ? List.of() : placementResult.getUnplacedFurniture();
        this.warningCode = placementResult == null ? null : placementResult.getWarningCode();
        this.message = placementResult == null ? null : placementResult.getMessage();
    }

    public static LayoutResponse ofRecommendation(Layout layout, PlacementResult placementResult,
                                                   ValidationResult validationResult) {
        return new LayoutResponse(layout, placementResult.getStatus(),
                placementResult.getScoreSummary(), validationResult, placementResult);
    }

    public static LayoutResponse ofRecommendationFailure(Long roomId, PlacementResult placementResult,
                                                          ValidationResult validationResult) {
        return new LayoutResponse(null, placementResult.getStatus(), placementResult.getScoreSummary(),
                validationResult, placementResult, roomId);
    }

    public static LayoutResponse ofUpdate(Layout layout, RecommendationStatus status,
                                           ScoreSummary scoreSummary, ValidationResult validationResult) {
        return new LayoutResponse(layout, status, scoreSummary, validationResult, null);
    }

    public static LayoutResponse ofSnapshot(Layout layout, ScoreSummary scoreSummary,
                                            ValidationResult validationResult) {
        return new LayoutResponse(layout, RecommendationStatus.SUCCESS, scoreSummary, validationResult, null);
    }

    private LayoutResponse(Layout layout, RecommendationStatus status, ScoreSummary scoreSummary,
                           ValidationResult validationResult, PlacementResult placementResult, Long roomId) {
        this.layoutId = null;
        this.roomId = roomId;
        this.sourceLayoutId = null;
        this.confirmed = false;
        this.confirmedAt = null;
        this.status = status;
        this.recommendedFurniture = List.of();
        this.scoreSummary = scoreSummary;
        this.validationResult = validationResult;
        this.recommendationStatus = placementResult.getRecommendationStatus();
        this.requestedFurnitureCount = placementResult.getRequestedFurnitureCount();
        this.placedFurnitureCount = placementResult.getPlacedFurnitureCount();
        this.unplacedFurniture = placementResult.getUnplacedFurniture();
        this.warningCode = placementResult.getWarningCode();
        this.message = placementResult.getMessage();
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

    public RecommendationExecutionStatus getRecommendationStatus() { return recommendationStatus; }
    public int getRequestedFurnitureCount() { return requestedFurnitureCount; }
    public int getPlacedFurnitureCount() { return placedFurnitureCount; }
    public List<com.roomfit.placement.UnplacedFurniture> getUnplacedFurniture() { return unplacedFurniture; }
    public String getWarningCode() { return warningCode; }
    public String getMessage() { return message; }
}
