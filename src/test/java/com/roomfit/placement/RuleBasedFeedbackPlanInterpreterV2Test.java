package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.domain.DesignStyle;
import com.roomfit.agent.domain.LifestyleGoal;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
    void resolvesTheSingleActiveRemovalTargetToItsExistingId() {
        FeedbackPlan plan = interpret("소파를 없애줘");

        FeedbackOperation operation = plan.operations().getFirst();
        assertThat(operation.type()).isEqualTo(FeedbackOperationType.REMOVE_FURNITURE);
        assertThat(operation.target().furnitureType()).isEqualTo("sofa");
        assertThat(operation.target().furnitureId()).isEqualTo("sofa-1");
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
    void normalizesLightingAndDoesNotGuessAmbiguousTableTerms() {
        FeedbackOperation lighting = interpret("구석에 조명을 추가해줘").operations().getFirst();
        FeedbackPlan table = interpret("작은 테이블을 추가해줘");

        assertThat(lighting.target().furnitureType()).isEqualTo("mood_lamp");
        assertThat(lighting.placement().relation()).isEqualTo(FeedbackRelation.IN_CORNER);
        assertThat(table.needsClarification()).isTrue();
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
    void doesNotInventANonCanonicalStorageFurnitureType() {
        FeedbackPlan plan = interpret("수납장을 하나 더 추가해줘");

        assertThat(plan.needsClarification()).isTrue();
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
        assertThat(interpret("의자를 빼고 책상을 옮겨줘").needsClarification()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"소파를 삭제해줘", "소파를 없애줘", "소파를 치워줘", "소파를 빼줘", "소파는 필요 없어"})
    void recognizesKoreanRemovalSynonyms(String feedback) {
        FeedbackPlan plan = interpreter.interpret(feedback, room(), List.of(furniture("sofa-1", "sofa", 4, 4)), context());

        assertThat(plan.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.REMOVE_FURNITURE);
            assertThat(operation.target().furnitureId()).isEqualTo("sofa-1");
        });
    }

    @Test
    void movesExistingChairWithoutCreatingAnAddOperation() {
        Furniture chair = furniture("chair-1", "desk_chair", 2, 2);
        FeedbackPlan plan = interpreter.interpret("의자를 오른쪽으로 배치해줘", room(), List.of(chair), context());

        assertThat(plan.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE);
            assertThat(operation.target().furnitureId()).isEqualTo("chair-1");
            assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.RIGHT);
        });
    }

    @Test
    void returnsClarificationForCornerMoveInsteadOfChangingItIntoAnAdd() {
        Furniture chair = furniture("chair-1", "desk_chair", 2, 2);
        FeedbackPlan plan = interpreter.interpret("의자를 구석에 배치해줘", room(), List.of(chair), context());

        assertThat(plan.needsClarification()).isTrue();
        assertThat(plan.operations()).isEmpty();
        assertThat(plan.clarification().targetFurnitureType()).isEqualTo("desk_chair");
    }

    @Test
    void splitsMoveAndRemoveIntoTwoOperations() {
        FeedbackPlan plan = interpreter.interpret("침대는 왼쪽 벽 쪽으로 옮기고 소파는 삭제해줘", room(), List.of(
                furniture("bed-1", "bed", 2, 2), furniture("sofa-1", "sofa", 4, 4)), context());

        assertThat(plan.requestKind()).isEqualTo(FeedbackRequestKind.COMPOSITE);
        assertThat(plan.operations()).extracting(FeedbackOperation::type)
                .containsExactly(FeedbackOperationType.MOVE, FeedbackOperationType.REMOVE_FURNITURE);
    }

    @Test
    void swapsSameTypeForDifferentDesignWhileKeepingCanonicalType() {
        FeedbackPlan plan = interpreter.interpret("침대를 다른 디자인으로 바꿔줘", room(),
                List.of(furniture("bed-1", "bed", 2, 2)), context());

        assertThat(plan.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.SWAP_FURNITURE);
            assertThat(operation.target().furnitureId()).isEqualTo("bed-1");
            assertThat(operation.replacementRequirements().furnitureType()).isEqualTo("bed");
        });
    }

    @Test
    void describesMultipleSameTypeCandidatesWithoutExposingIds() {
        FeedbackPlan plan = interpreter.interpret("책상을 옮겨줘", room(), List.of(
                new Furniture("desk-1", "desk", "창가 책상", 1, 1, 0.8, new Position(1, 1), 0, FurnitureStatus.EXISTING),
                new Furniture("desk-2", "desk", "모던 책상", 1, 1, 0.8, new Position(4, 4), 0, FurnitureStatus.EXISTING)), context());

        assertThat(plan.needsClarification()).isTrue();
        assertThat(plan.clarification().question()).contains("창가 책상", "모던 책상")
                .doesNotContain("desk-1", "desk-2");
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
