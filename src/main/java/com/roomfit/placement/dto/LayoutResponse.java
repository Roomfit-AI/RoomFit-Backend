package com.roomfit.placement.dto;

import com.roomfit.placement.Layout;
import com.roomfit.placement.PlacementResult;
import com.roomfit.placement.RecommendationStatus;
import com.roomfit.placement.ScoreSummary;
import com.roomfit.placement.ValidationResult;
import com.roomfit.room.Furniture;

import java.util.List;

public class LayoutResponse {

    private final Long layoutId;
    private final RecommendationStatus status;
    private final List<Furniture> recommendedFurniture;
    private final ScoreSummary scoreSummary;
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
