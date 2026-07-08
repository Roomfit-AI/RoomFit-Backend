package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.room.Furniture;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ScoreService {

    public ScoreSummary calculate(AgentContext context, List<Furniture> furniture,
                                   ValidationResult validationResult) {
        int collisionScore = validationResult.isCollisionFree() ? 100 : 60;
        int boundaryScore = validationResult.isBoundaryValid() ? 100 : 60;
        int doorWindowScore = validationResult.isDoorClearance() && validationResult.isWindowClearance() ? 100 : 70;
        int pathScore = validationResult.isPathSecured() ? 100 : 70;
        int goalScore = calculateGoalScore(context, furniture);
        int styleScore = calculateStyleScore(context, furniture);

        return new ScoreSummary(collisionScore, boundaryScore, doorWindowScore,
                pathScore, goalScore, styleScore);
    }

    private int calculateGoalScore(AgentContext context, List<Furniture> furniture) {
        if (context.getLifestyleGoal() != LifestyleGoal.STUDY_FOCUSED) {
            return 80;
        }

        Set<String> furnitureTypes = furniture.stream()
                .map(Furniture::getType)
                .collect(Collectors.toSet());

        return furnitureTypes.containsAll(Set.of("desk", "chair", "lamp")) ? 95 : 80;
    }

    private int calculateStyleScore(AgentContext context, List<Furniture> furniture) {
        Set<String> contextTags = Set.copyOf(context.getStyleTags());
        Set<String> furnitureTags = furniture.stream()
                .flatMap(item -> item.getStyleTags().stream())
                .collect(Collectors.toSet());

        if (contextTags.isEmpty() || furnitureTags.isEmpty()) {
            return 80;
        }

        long overlapCount = furnitureTags.stream()
                .filter(contextTags::contains)
                .count();

        if (overlapCount >= 3) {
            return 95;
        }
        if (overlapCount > 0) {
            return 90;
        }
        return 80;
    }
}
