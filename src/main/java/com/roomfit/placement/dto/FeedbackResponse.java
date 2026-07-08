package com.roomfit.placement.dto;

import com.roomfit.placement.Layout;
import com.roomfit.placement.RecommendationStatus;
import com.roomfit.placement.ScoreSummary;
import com.roomfit.placement.ValidationResult;
import com.roomfit.room.Furniture;

import java.util.List;
import java.util.Map;

public class FeedbackResponse {

    private final Long layoutId;
    private final RecommendationStatus status;
    private final List<Furniture> recommendedFurniture;
    private final ScoreSummary scoreSummary;
    private final ValidationResult validationResult;
    private final Map<String, Object> interpretedIntent;

    private FeedbackResponse(Long layoutId, RecommendationStatus status, List<Furniture> recommendedFurniture,
                              ScoreSummary scoreSummary, ValidationResult validationResult,
                              Map<String, Object> interpretedIntent) {
        this.layoutId = layoutId;
        this.status = status;
        this.recommendedFurniture = recommendedFurniture;
        this.scoreSummary = scoreSummary;
        this.validationResult = validationResult;
        this.interpretedIntent = interpretedIntent;
    }

    public static FeedbackResponse of(Layout layout, RecommendationStatus status, ScoreSummary scoreSummary,
                                       ValidationResult validationResult, Map<String, Object> interpretedIntent) {
        return new FeedbackResponse(layout.getId(), status, layout.getFurniture(),
                scoreSummary, validationResult, interpretedIntent);
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

    public Map<String, Object> getInterpretedIntent() {
        return interpretedIntent;
    }
}
