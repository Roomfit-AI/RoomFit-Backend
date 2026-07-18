package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RuleBasedFeedbackPlanInterpreterV2Test {

    private final RuleBasedFeedbackPlanInterpreter interpreter = new RuleBasedFeedbackPlanInterpreter();

    @Test
    void parsesGenericAddWithReferenceAndSmallSizePreference() {
        FeedbackPlan plan = interpret("침대 옆에 작은 협탁 하나 추가해줘");

        FeedbackOperation operation = plan.operations().getFirst();
        assertThat(operation.type()).isEqualTo(FeedbackOperationType.ADD_FURNITURE);
        assertThat(operation.target().furnitureType()).isEqualTo("nightstand");
        assertThat(operation.referenceTarget().furnitureType()).isEqualTo("bed");
        assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.NEXT_TO);
        assertThat(operation.productRequirements().sizePreference()).isEqualTo(FeedbackSizePreference.SMALL);
    }

    @Test
    void parsesGenericRemoveWithoutChoosingAnExistingIndex() {
        FeedbackPlan plan = interpret("소파를 없애줘");

        FeedbackOperation operation = plan.operations().getFirst();
        assertThat(operation.type()).isEqualTo(FeedbackOperationType.REMOVE_FURNITURE);
        assertThat(operation.target().furnitureType()).isEqualTo("sofa");
        assertThat(operation.target().furnitureId()).isBlank();
    }

    @Test
    void parsesGenericSwapFromSourceAndReplacementTypes() {
        FeedbackPlan plan = interpret("책장을 빼고 행거를 넣어줘");

        FeedbackOperation operation = plan.operations().getFirst();
        assertThat(operation.type()).isEqualTo(FeedbackOperationType.SWAP_FURNITURE);
        assertThat(operation.target().furnitureType()).isEqualTo("bookshelf");
        assertThat(operation.replacementRequirements().furnitureType()).isEqualTo("hanger");
    }

    @Test
    void preservesAmbiguousExistingDeskClarificationPolicy() {
        List<Furniture> twoDesks = List.of(
                furniture("desk-1", "desk", 1, 1),
                furniture("desk-2", "desk", 4, 4)
        );

        FeedbackPlan plan = interpreter.interpret("책상 더 크게", room(), twoDesks, context());

        assertThat(plan.requestKind()).isEqualTo(FeedbackRequestKind.CLARIFICATION);
        assertThat(plan.operations()).isEmpty();
    }

    @Test
    void doesNotForceRemoveAndMovePhraseIntoSwap() {
        assertThatThrownBy(() -> interpret("의자를 빼고 책상을 옮겨줘"))
                .isInstanceOf(com.roomfit.common.CustomException.class);
    }

    private FeedbackPlan interpret(String feedback) {
        return interpreter.interpret(feedback, room(), List.of(
                furniture("bed-1", "bed", 1.5, 2),
                furniture("sofa-1", "sofa", 4.5, 2),
                furniture("bookshelf-1", "bookshelf", 5, 4)
        ), context());
    }

    private Furniture furniture(String id, String type, double x, double z) {
        return new Furniture(id, type, type, 0.8, 0.5, 0.8,
                new Position(x, z), 0, FurnitureStatus.EXISTING);
    }

    private Room room() {
        return new Room(null, 6, 6, 2.4, "meter", List.of(), List.of());
    }

    private AgentContext context() {
        return new AgentContext(1L, LifestyleGoal.STUDY_FOCUSED, List.of(DesignStyle.MINIMAL),
                List.of(), List.of(), List.of(1L), List.of(), List.of("minimal"));
    }
}
