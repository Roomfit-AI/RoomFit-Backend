package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ScoreServiceDeletedFurnitureTest {

    private final ScoreService scoreService = new ScoreService();

    @Test
    void deletedFurnitureDoesNotContributeToGoalOrStyleScores() {
        AgentContext context = new AgentContext(
                1L,
                LifestyleGoal.STUDY_FOCUSED,
                List.of(DesignStyle.MINIMAL),
                List.of("desk", "chair", "lamp"),
                List.of(),
                List.of(1L),
                List.of(),
                List.of("minimal")
        );
        ValidationResult validation = new ValidationResult(true, true, true, true, true, List.of(), List.of());
        List<Furniture> furniture = List.of(
                furniture("desk", FurnitureStatus.DELETED, List.of("minimal")),
                furniture("desk_chair", FurnitureStatus.DELETED, List.of("minimal")),
                furniture("mood_lamp", FurnitureStatus.DELETED, List.of("minimal"))
        );

        ScoreSummary score = scoreService.calculate(context, furniture, validation);

        assertThat(score.getGoalScore()).isEqualTo(80);
        assertThat(score.getStyleScore()).isEqualTo(80);
    }

    private Furniture furniture(String type, FurnitureStatus status, List<String> styleTags) {
        return new Furniture(type + "-1", type, type, 0.5, 0.5, 0.5,
                new Position(1, 1), 0, status, null, styleTags, null);
    }
}
