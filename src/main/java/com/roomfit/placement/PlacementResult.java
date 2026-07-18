package com.roomfit.placement;

import com.roomfit.room.Furniture;

import java.util.ArrayList;
import java.util.List;

public class PlacementResult {

    private final RecommendationStatus status;
    private final List<Furniture> recommendedFurniture;
    private final ScoreSummary scoreSummary;
    private final int requestedFurnitureCount;
    private final int placedFurnitureCount;
    private final List<UnplacedFurniture> unplacedFurniture;
    private final RecommendationExecutionStatus recommendationStatus;
    private final String warningCode;
    private final String message;

    public PlacementResult(RecommendationStatus status, List<Furniture> recommendedFurniture) {
        this(status, recommendedFurniture, ScoreSummary.defaultSummary());
    }

    public PlacementResult(RecommendationStatus status, List<Furniture> recommendedFurniture,
                            ScoreSummary scoreSummary) {
        this(status, recommendedFurniture, scoreSummary, 0, 0, List.of(),
                RecommendationExecutionStatus.SUCCESS, null, "추천 가구를 배치했습니다.");
    }

    public PlacementResult(RecommendationStatus status, List<Furniture> recommendedFurniture,
                           ScoreSummary scoreSummary, int requestedFurnitureCount, int placedFurnitureCount,
                           List<UnplacedFurniture> unplacedFurniture,
                           RecommendationExecutionStatus recommendationStatus,
                           String warningCode, String message) {
        this.status = status;
        // Layout's JPA ElementCollection owns this list after a recommendation
        // is saved, so it must remain mutable.
        this.recommendedFurniture = new ArrayList<>(recommendedFurniture);
        this.scoreSummary = scoreSummary;
        this.requestedFurnitureCount = requestedFurnitureCount;
        this.placedFurnitureCount = placedFurnitureCount;
        this.unplacedFurniture = List.copyOf(unplacedFurniture);
        this.recommendationStatus = recommendationStatus;
        this.warningCode = warningCode;
        this.message = message;
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

    public int getRequestedFurnitureCount() {
        return requestedFurnitureCount;
    }

    public int getPlacedFurnitureCount() {
        return placedFurnitureCount;
    }

    public List<UnplacedFurniture> getUnplacedFurniture() {
        return unplacedFurniture;
    }

    public RecommendationExecutionStatus getRecommendationStatus() {
        return recommendationStatus;
    }

    public String getWarningCode() {
        return warningCode;
    }

    public String getMessage() {
        return message;
    }
}
