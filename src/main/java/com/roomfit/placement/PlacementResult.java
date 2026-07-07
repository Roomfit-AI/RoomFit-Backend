package com.roomfit.placement;

import com.roomfit.room.Furniture;

import java.util.List;

public class PlacementResult {

    private final RecommendationStatus status;
    private final List<Furniture> recommendedFurniture;

    public PlacementResult(RecommendationStatus status, List<Furniture> recommendedFurniture) {
        this.status = status;
        this.recommendedFurniture = recommendedFurniture;
    }

    public RecommendationStatus getStatus() {
        return status;
    }

    public List<Furniture> getRecommendedFurniture() {
        return recommendedFurniture;
    }
}
