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
    void normalizesLightingAndTableTermsToCatalogTypes() {
        FeedbackOperation lighting = interpret("구석에 조명을 추가해줘").operations().getFirst();
        FeedbackOperation table = interpret("작은 테이블을 추가해줘").operations().getFirst();

        assertThat(lighting.target().furnitureType()).isEqualTo("mood_lamp");
        assertThat(lighting.placement().relation()).isEqualTo(FeedbackRelation.IN_CORNER);
        assertThat(table.target().furnitureType()).isEqualTo("multi_table");
        assertThat(table.productRequirements().sizePreference()).isEqualTo(FeedbackSizePreference.SMALL);
    }

    @Test
    void parsesSameTypeSmallerSofaAsCatalogSwap() {
        FeedbackOperation operation = interpret("작은 소파로 바꿔줘").operations().getFirst();

        assertThat(operation.type()).isEqualTo(FeedbackOperationType.SWAP_FURNITURE);
        assertThat(operation.target().furnitureType()).isEqualTo("sofa");
        assertThat(operation.replacementRequirements().furnitureType()).isEqualTo("sofa");
        assertThat(operation.replacementRequirements().sizePreference()).isEqualTo(FeedbackSizePreference.SMALL);
    }

    @Test
    void keepsGenericStorageSeparateFromBookshelfHangerAndWardrobe() {
        FeedbackOperation operation = interpret("수납장을 하나 더 추가해줘").operations().getFirst();

        assertThat(operation.target().furnitureType()).isEqualTo("storage");
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

    @Test
    void normalizesRotateAndMoveAliasesForOneUniqueTarget() {
        List<Furniture> desk = List.of(furniture("desk-1", "desk", 3, 3));

        FeedbackPlan clockwise = interpreter.interpret("책상을 오른쪽으로 90도", room(), desk, context());
        FeedbackPlan counterClockwise = interpreter.interpret("책상을 반시계 방향으로 돌려줘", room(), desk, context());
        FeedbackPlan toward = interpreter.interpret("책상을 앞으로 당겨줘", room(), desk, context());
        FeedbackPlan away = interpreter.interpret("책상을 뒤로 밀어줘", room(), desk, context());

        assertThat(clockwise.operations().getFirst().placement().orientation())
                .isEqualTo(FeedbackOrientation.QUARTER_TURN_CW);
        assertThat(counterClockwise.operations().getFirst().placement().orientation())
                .isEqualTo(FeedbackOrientation.QUARTER_TURN_CCW);
        assertThat(toward.operations().getFirst().placement().relation()).isEqualTo(FeedbackRelation.FORWARD);
        assertThat(away.operations().getFirst().placement().relation()).isEqualTo(FeedbackRelation.BACKWARD);
    }

    @Test
    void normalizesExplicitLargerAndSmallerProductRequestsToReplaceProduct() {
        List<Furniture> desk = List.of(furniture("desk-1", "desk", 3, 3));

        FeedbackPlan larger = interpreter.interpret("책상을 더 큰 제품으로 바꿔줘", room(), desk, context());
        FeedbackPlan smaller = interpreter.interpret("책상을 더 작은 제품으로 바꿔줘", room(), desk, context());

        assertThat(larger.operations().getFirst().type()).isEqualTo(FeedbackOperationType.REPLACE_PRODUCT);
        assertThat(larger.operations().getFirst().constraints().largerThanCurrent()).isTrue();
        assertThat(smaller.operations().getFirst().type()).isEqualTo(FeedbackOperationType.REPLACE_PRODUCT);
        assertThat(smaller.operations().getFirst().constraints().smallerThanCurrent()).isTrue();
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
