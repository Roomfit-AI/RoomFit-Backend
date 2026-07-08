package com.roomfit.placement;

import com.roomfit.room.Furniture;

import java.util.List;

public class PlacementResult {

    private final RecommendationStatus status;
    private final List<Furniture> recommendedFurniture;
    private final ScoreSummary scoreSummary;

    public PlacementResult(RecommendationStatus status, List<Furniture> recommendedFurniture) {
        this(status, recommendedFurniture, ScoreSummary.defaultSummary());
    }

    public PlacementResult(RecommendationStatus status, List<Furniture> recommendedFurniture,
                            ScoreSummary scoreSummary) {
        this.status = status;
        this.recommendedFurniture = recommendedFurniture;
        this.scoreSummary = scoreSummary;
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
}
