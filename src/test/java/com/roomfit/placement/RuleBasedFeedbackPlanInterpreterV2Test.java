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
    void clarifiesCrossTypeSwapInsteadOfChangingCanonicalType() {
        FeedbackPlan plan = interpret("책장을 빼고 행거를 넣어줘");

        assertThat(plan.needsClarification()).isTrue();
        assertThat(plan.operations()).isEmpty();
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
    void resolvesCornerMoveForExistingChairWithoutChangingItIntoAnAdd() {
        Furniture chair = furniture("chair-1", "desk_chair", 2, 2);
        FeedbackPlan plan = interpreter.interpret("의자를 구석에 배치해줘", room(), List.of(chair), context());

        assertThat(plan.needsClarification()).isFalse();
        assertThat(plan.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE);
            assertThat(operation.target().furnitureId()).isEqualTo("chair-1");
            assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.IN_CORNER);
            assertThat(operation.placement().magnitude()).isNull();
        });
    }

    @Test
    void treatsImplicitPlacementWordsAsMoveOnlyForOneExistingFurniture() {
        Furniture chair = furniture("chair-1", "desk_chair", 2, 2);
        Furniture sofa = furniture("sofa-1", "sofa", 3, 3);

        FeedbackPlan chairMove = interpreter.interpret("의자를 구석에 넣어줘", room(), List.of(chair), context());
        FeedbackPlan sofaMove = interpreter.interpret("소파를 창가에 배치해줘", room(), List.of(sofa), context());
        FeedbackPlan wishMove = interpreter.interpret("의자가 구석에 있었으면 좋겠", room(), List.of(chair), context());
        FeedbackPlan explicitAdd = interpreter.interpret("의자 하나 더 구석에 넣어줘", room(), List.of(chair), context());
        FeedbackPlan modifiedMove = interpreter.interpret("의자를 추가로 이동해줘", room(), List.of(chair), context());
        FeedbackPlan quantifiedMove = interpreter.interpret("의자를 하나 더 옮겨줘", room(), List.of(chair), context());
        FeedbackPlan quantifiedAdd = interpreter.interpret("의자 하나 더 놔줘", room(), List.of(chair), context());
        FeedbackPlan absentChair = interpreter.interpret("의자를 구석에 넣어줘", room(), List.of(), context());
        FeedbackPlan ambiguousChairs = interpreter.interpret("의자를 구석에 넣어줘", room(),
                List.of(chair, furniture("chair-2", "desk_chair", 4, 4)), context());
        FeedbackPlan absentExplicitAdd = interpreter.interpret("의자 하나 추가해줘", room(), List.of(), context());

        assertThat(chairMove.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE);
            assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.IN_CORNER);
        });
        assertThat(sofaMove.operations()).singleElement().satisfies(operation ->
                assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE));
        assertThat(wishMove.operations()).singleElement().satisfies(operation ->
                assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE));
        assertThat(explicitAdd.operations()).singleElement().satisfies(operation ->
                assertThat(operation.type()).isEqualTo(FeedbackOperationType.ADD_FURNITURE));
        assertThat(modifiedMove.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE);
            assertThat(operation.target().furnitureId()).isEqualTo("chair-1");
        });
        assertThat(quantifiedMove.operations()).singleElement().satisfies(operation ->
                assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE));
        assertThat(quantifiedAdd.operations()).singleElement().satisfies(operation ->
                assertThat(operation.type()).isEqualTo(FeedbackOperationType.ADD_FURNITURE));
        assertThat(absentChair.needsClarification()).isTrue();
        assertThat(ambiguousChairs.needsClarification()).isTrue();
        assertThat(absentExplicitAdd.operations()).singleElement().satisfies(operation ->
                assertThat(operation.type()).isEqualTo(FeedbackOperationType.ADD_FURNITURE));
    }

    @Test
    void usesActiveSelectionForGenericCornerMoveAndClarifiesWithoutIt() {
        Furniture chair = furniture("chair-1", "desk_chair", 2, 2);

        FeedbackPlan selected = interpreter.interpret("가구를 모서리에 배치해줘", room(), List.of(chair), context(), "chair-1");
        FeedbackPlan noSelection = interpreter.interpret("가구를 모서리에 배치해줘", room(), List.of(chair), context(), null);

        assertThat(selected.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE);
            assertThat(operation.target().furnitureId()).isEqualTo("chair-1");
            assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.IN_CORNER);
        });
        assertThat(noSelection.needsClarification()).isTrue();
        assertThat(noSelection.operations()).isEmpty();
    }

    @Test
    void movesExistingFurnitureNearAnExplicitReferenceWithoutAddingFurniture() {
        Furniture monitor = furniture("monitor-1", "monitor", 1, 1);
        Furniture desk = furniture("desk-1", "desk", 3, 3);

        FeedbackPlan plan = interpreter.interpret("모니터를 책상 가까이로 옮겨줘", room(), List.of(monitor, desk), context());

        assertThat(plan.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE);
            assertThat(operation.target().furnitureId()).isEqualTo("monitor-1");
            assertThat(operation.referenceTarget().furnitureId()).isEqualTo("desk-1");
            assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.NEXT_TO);
        });
    }

    @Test
    void preservesLeftAndRightReferenceRolesWithoutSelectingTheDeskAsTarget() {
        Furniture chair = furniture("chair-1", "desk_chair", 1, 1);
        Furniture desk = furniture("desk-1", "desk", 3, 3);

        FeedbackPlan left = interpreter.interpret("책상 왼쪽에 의자를 옮겨줘", room(), List.of(chair, desk), context());
        FeedbackPlan right = interpreter.interpret("의자를 책상 오른쪽으로 옮겨줘", room(), List.of(chair, desk), context());

        assertThat(left.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.target().furnitureId()).isEqualTo("chair-1");
            assertThat(operation.referenceTarget().furnitureId()).isEqualTo("desk-1");
            assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.LEFT_OF);
        });
        assertThat(right.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.target().furnitureId()).isEqualTo("chair-1");
            assertThat(operation.referenceTarget().furnitureId()).isEqualTo("desk-1");
            assertThat(operation.placement().relation()).isEqualTo(FeedbackRelation.RIGHT_OF);
        });
    }

    @Test
    void removesOnlyTheSelectedFurnitureWhenSameTypeCandidatesExist() {
        Furniture first = furniture("chair-1", "desk_chair", 1, 1);
        Furniture second = furniture("chair-2", "desk_chair", 3, 3);

        FeedbackPlan selected = interpreter.interpret("의자를 삭제해줘", room(), List.of(first, second), context(), "chair-2");
        FeedbackPlan ambiguous = interpreter.interpret("의자를 삭제해줘", room(), List.of(first, second), context());

        assertThat(selected.operations()).singleElement().satisfies(operation ->
                assertThat(operation.target().furnitureId()).isEqualTo("chair-2"));
        assertThat(ambiguous.needsClarification()).isTrue();
    }

    @Test
    void clarifiesWhenSeveralFurnitureTypesAreMovedWithoutSeparateTargets() {
        Furniture chair = furniture("chair-1", "desk_chair", 2, 2);
        Furniture desk = furniture("desk-1", "desk", 4, 2);

        FeedbackPlan plan = interpreter.interpret("의자와 책상을 옮겨줘", room(), List.of(chair, desk), context());

        assertThat(plan.needsClarification()).isTrue();
        assertThat(plan.operations()).isEmpty();
    }

    @Test
    void clarifiesReferenceMovesThatAlsoSpecifyAnAbsoluteDestination() {
        Furniture chair = furniture("chair-1", "desk_chair", 2, 2);
        Furniture desk = furniture("desk-1", "desk", 4, 2);

        FeedbackPlan window = interpreter.interpret("책상 옆 의자를 창문 쪽으로 옮겨줘",
                room(), List.of(chair, desk), context());
        FeedbackPlan wall = interpreter.interpret("책상 옆 의자를 벽 쪽으로 옮겨줘",
                room(), List.of(chair, desk), context());

        assertThat(window.needsClarification()).isTrue();
        assertThat(window.operations()).isEmpty();
        assertThat(wall.needsClarification()).isTrue();
        assertThat(wall.operations()).isEmpty();
    }

    @Test
    void mapsKnownToneAndMaterialTermsToActualCatalogMetadataKeywords() {
        Furniture drawer = furniture("drawer-1", "drawer_chest", 2, 2);
        Furniture chair = furniture("chair-1", "desk_chair", 4, 2);

        FeedbackPlan lightDrawer = interpreter.interpret("밝은 색 수납장으로 바꿔줘", room(), List.of(drawer), context());
        FeedbackPlan metalChair = interpreter.interpret("금속 소재 의자로 바꿔줘", room(), List.of(chair), context());

        assertThat(lightDrawer.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.SWAP_FURNITURE);
            assertThat(operation.replacementRequirements().styleKeywords()).containsExactly("paintedWhite");
        });
        assertThat(metalChair.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.SWAP_FURNITURE);
            assertThat(operation.replacementRequirements().styleKeywords()).containsExactly("metal");
        });
    }

    @Test
    void usesOnlyActiveSelectedFurnitureForTypeOmittedMetadataSwap() {
        Furniture drawer = furniture("drawer-1", "drawer_chest", 2, 2);
        Furniture deleted = new Furniture("drawer-deleted", "drawer_chest", "deleted", 0.8, 0.5, 0.8,
                new Position(4, 2), 0, FurnitureStatus.DELETED);

        FeedbackPlan selected = interpreter.interpret("우드 톤으로 바꿔줘", room(), List.of(drawer), context(), "drawer-1");
        FeedbackPlan noSelection = interpreter.interpret("우드 톤으로 바꿔줘", room(), List.of(drawer), context(), null);
        FeedbackPlan missingSelection = interpreter.interpret("우드 톤으로 바꿔줘", room(), List.of(drawer), context(), "missing");
        FeedbackPlan deletedSelection = interpreter.interpret("우드 톤으로 바꿔줘", room(), List.of(drawer, deleted), context(), "drawer-deleted");

        assertThat(selected.operations()).singleElement().satisfies(operation -> {
            assertThat(operation.type()).isEqualTo(FeedbackOperationType.SWAP_FURNITURE);
            assertThat(operation.target().furnitureId()).isEqualTo("drawer-1");
            assertThat(operation.replacementRequirements().styleKeywords()).containsExactly("wood");
        });
        assertThat(noSelection.needsClarification()).isTrue();
        assertThat(missingSelection.needsClarification()).isTrue();
        assertThat(deletedSelection.needsClarification()).isTrue();
    }

    @Test
    void rejectsExplicitSwapWhenTheSelectedFurnitureHasAnotherCanonicalType() {
        Furniture drawer = furniture("drawer-1", "drawer_chest", 2, 2);
        Furniture chair = furniture("chair-1", "desk_chair", 4, 2);

        FeedbackPlan plan = interpreter.interpret("의자를 우드 톤으로 바꿔줘", room(), List.of(drawer, chair), context(), "drawer-1");

        assertThat(plan.needsClarification()).isTrue();
        assertThat(plan.operations()).isEmpty();
    }

    @Test
    void preservesCompoundOperationOrderForMoveSwapAndReferenceMoveRemove() {
        Furniture bed = furniture("bed-1", "bed", 1, 1);
        Furniture chair = furniture("chair-1", "desk_chair", 4, 2);
        Furniture desk = furniture("desk-1", "desk", 3, 3);
        Furniture monitor = furniture("monitor-1", "monitor", 1, 3);

        FeedbackPlan moveSwap = interpreter.interpret("침대는 창가 쪽으로 옮겨 주고 의자는 우드 톤으로 바꿔줘",
                room(), List.of(bed, chair), context());
        FeedbackPlan moveRemove = interpreter.interpret("책상 옆에 모니터를 옮긴 다음 기존 의자를 삭제해줘",
                room(), List.of(desk, monitor, chair), context());

        assertThat(moveSwap.operations()).extracting(FeedbackOperation::type)
                .containsExactly(FeedbackOperationType.MOVE, FeedbackOperationType.SWAP_FURNITURE);
        assertThat(moveSwap.operations().get(1).target().furnitureId()).isEqualTo("chair-1");
        assertThat(moveRemove.operations()).extracting(FeedbackOperation::type)
                .containsExactly(FeedbackOperationType.MOVE, FeedbackOperationType.REMOVE_FURNITURE);
        assertThat(moveRemove.operations().getFirst().target().furnitureId()).isEqualTo("monitor-1");
        assertThat(moveRemove.operations().getFirst().referenceTarget().furnitureId()).isEqualTo("desk-1");
        assertThat(moveRemove.operations().get(1).target().furnitureId()).isEqualTo("chair-1");
    }

    @Test
    void rejectsUnsupportedToneAndOverFourCompoundOperationsWithoutPartialPlan() {
        Furniture bed = furniture("bed-1", "bed", 1, 1);
        Furniture chair = furniture("chair-1", "desk_chair", 2, 2);
        Furniture desk = furniture("desk-1", "desk", 3, 3);
        Furniture sofa = furniture("sofa-1", "sofa", 4, 4);
        Furniture monitor = furniture("monitor-1", "monitor", 5, 5);

        FeedbackPlan unsupportedTone = interpreter.interpret("의자를 다크 톤으로 바꿔줘", room(), List.of(chair), context());
        FeedbackPlan tooMany = interpreter.interpret("침대를 옮기고 의자를 옮기고 책상을 옮기고 소파를 옮기고 모니터를 옮겨줘",
                room(), List.of(bed, chair, desk, sofa, monitor), context());

        assertThat(unsupportedTone.needsClarification()).isTrue();
        assertThat(tooMany.needsClarification()).isTrue();
        assertThat(tooMany.operations()).isEmpty();
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
