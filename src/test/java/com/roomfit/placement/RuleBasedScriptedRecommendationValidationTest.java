package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.product.service.MockProductService;
import com.roomfit.product.service.ProductRecommendationService;
import com.roomfit.room.Furniture;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RuleBasedScriptedRecommendationValidationTest {

    private final ValidationService validationService = new ValidationService();
    private final RuleBasedPlacementService service = new RuleBasedPlacementService(
            mock(MockProductService.class), mock(ProductRecommendationService.class), validationService);

    @Test
    void scriptedDemo_usesActualValidatorForPartialAndFailedOutcomes() {
        Room partialRoom = studio(List.of());
        Room failedRoom = new Room(null, "샘플룸2", 1.0, 1.0, 2.8,
                "meter", List.of(), List.of(), List.of(), null, null, null);
        PlacementResult partial = service.recommend(context(), partialRoom);
        PlacementResult failed = service.recommend(context(), failedRoom);

        assertOutcome(partialRoom, partial, RecommendationExecutionStatus.PARTIAL_SUCCESS, false, true);
        assertOutcome(failedRoom, failed, RecommendationExecutionStatus.FAILED, false, false);
    }

    private void assertOutcome(Room room, PlacementResult result, RecommendationExecutionStatus expectedStatus,
                               boolean collisionFree, boolean boundaryValid) {
        ValidationResult validation = validationService.validate(room, result.getRecommendedFurniture());
        assertThat(result.getRecommendationStatus()).isEqualTo(expectedStatus);
        if (expectedStatus == RecommendationExecutionStatus.PARTIAL_SUCCESS) {
            assertThat(result.getWarningCode()).isEqualTo("LAYOUT_VALIDATION_FAILED");
        }
        assertThat(validation.isCollisionFree()).isEqualTo(collisionFree);
        assertThat(validation.isBoundaryValid()).isEqualTo(boundaryValid);
    }

    private AgentContext context() {
        return new AgentContext(1L, LifestyleGoal.RELAX_FOCUSED, List.of(DesignStyle.MODERN),
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    private Room studio(List<Furniture> furniture) {
        return new Room(null, "샘플룸2", 6.4, 5.8, 2.8, "meter", List.of(), List.of(), furniture,
                null, null, null);
    }
}
