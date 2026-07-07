package com.roomfit.placement.dto;

import com.roomfit.placement.Layout;
import com.roomfit.placement.RecommendationStatus;
import com.roomfit.placement.ValidationResult;
import com.roomfit.room.Furniture;

import java.util.List;

public class LayoutResponse {

    private final Long layoutId;
    private final RecommendationStatus status; // recommend 응답에서만 사용, update 응답에서는 null 가능
    private final List<Furniture> recommendedFurniture;
    private final ValidationResult validationResult;

    private LayoutResponse(Long layoutId, RecommendationStatus status,
                            List<Furniture> recommendedFurniture, ValidationResult validationResult) {
        this.layoutId = layoutId;
        this.status = status;
        this.recommendedFurniture = recommendedFurniture;
        this.validationResult = validationResult;
    }

    public static LayoutResponse ofRecommendation(Layout layout, RecommendationStatus status,
                                                   ValidationResult validationResult) {
        return new LayoutResponse(layout.getId(), status, layout.getFurniture(), validationResult);
    }

    public static LayoutResponse ofUpdate(Layout layout, ValidationResult validationResult) {
        return new LayoutResponse(layout.getId(), null, layout.getFurniture(), validationResult);
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

    public ValidationResult getValidationResult() {
        return validationResult;
    }
}
