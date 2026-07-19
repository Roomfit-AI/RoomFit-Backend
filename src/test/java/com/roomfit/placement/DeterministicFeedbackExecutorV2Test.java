package com.roomfit.placement;

import com.roomfit.product.repository.MockProductRepository;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureBoundary;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicFeedbackExecutorV2Test {

    private final DeterministicFeedbackExecutor executor =
            new DeterministicFeedbackExecutor(new ValidationService(), new MockProductRepository());

    @ParameterizedTest
    @CsvSource({
            "SMALL,2.2",
            "MEDIUM,2.4",
            "LARGE,2.6"
    })
    void semanticMoveMagnitudeUsesDeterministicBackendDistance(FeedbackMagnitude magnitude, double expectedX) {
        Furniture before = desk("desk-1", 2, 2, 0);

        FeedbackExecution execution = executor.execute(direct(move("op-1",
                new FeedbackTargetSelector("desk-1", "desk", ""), FeedbackRelation.RIGHT, magnitude)),
                room(6, 6), List.of(before));

        assertThat(execution.result().applied()).isTrue();
        assertThat(execution.furniture().getFirst().getPosition().getX()).isEqualTo(expectedX);
    }

    @ParameterizedTest
    @CsvSource({
            "QUARTER_TURN_CW,90",
            "QUARTER_TURN_CCW,270",
            "HALF_TURN,180"
    })
    void semanticOrientationUsesDeterministicBackendRotation(FeedbackOrientation orientation, double expectedRotation) {
        Furniture before = desk("desk-1", 2, 2, 0);

        FeedbackExecution execution = executor.execute(direct(rotate("op-1",
                new FeedbackTargetSelector("desk-1", "desk", ""), orientation, List.of())),
                room(6, 6), List.of(before));

        assertThat(execution.result().applied()).isTrue();
        assertThat(execution.furniture().getFirst().getRotation()).isEqualTo(expectedRotation);
    }

    @Test
    void alignWithWallTriesDeterministicCardinalCandidates() {
        Furniture before = desk("desk-1", 2, 2, 45);

        FeedbackExecution execution = executor.execute(direct(rotate("op-1",
                new FeedbackTargetSelector("desk-1", "desk", ""), FeedbackOrientation.ALIGN_WITH_WALL, List.of())),
                room(6, 6), List.of(before));

        assertThat(execution.result().applied()).isTrue();
        assertThat(execution.furniture().getFirst().getRotation()).isEqualTo(0);
    }

    @Test
    void compositeOperationsApplyInOrderToLatestSnapshotWithoutMutatingInput() {
        Furniture before = desk("desk-1", 2, 2, 0);
        FeedbackTargetSelector target = new FeedbackTargetSelector("", "desk", "");
        FeedbackOperation move = move("op-1", target, FeedbackRelation.RIGHT, FeedbackMagnitude.MEDIUM);
        FeedbackOperation rotate = rotate("op-2", target, FeedbackOrientation.QUARTER_TURN_CW, List.of("op-1"));
        FeedbackPlan plan = new FeedbackPlan("2.0", FeedbackRequestKind.COMPOSITE,
                List.of(move, rotate), List.of(), null, "move and rotate", FeedbackSource.LLM, false);

        FeedbackExecution execution = executor.execute(plan, room(6, 6), List.of(before));
        Furniture after = execution.furniture().getFirst();

        assertThat(execution.result().operationsApplied()).containsExactly("MOVE", "ROTATE");
        assertThat(after.getPosition().getX()).isEqualTo(2.4);
        assertThat(after.getRotation()).isEqualTo(90);
        assertThat(before.getPosition().getX()).isEqualTo(2);
        assertThat(before.getRotation()).isEqualTo(0);
    }

    @Test
    void furnitureIdTakesPriorityWhenMultipleFurnitureShareTheSameType() {
        Furniture first = desk("desk-1", 1, 1, 0);
        Furniture second = desk("desk-2", 4, 4, 0);

        FeedbackExecution execution = executor.execute(direct(move("op-1",
                new FeedbackTargetSelector("desk-2", "desk", ""), FeedbackRelation.RIGHT, FeedbackMagnitude.SMALL)),
                room(6, 6), List.of(first, second));

        assertThat(execution.furniture().get(0).getPosition().getX()).isEqualTo(1);
        assertThat(execution.furniture().get(1).getPosition().getX()).isEqualTo(4.2);
    }

    @Test
    void uniqueFurnitureTypeResolvesWithoutFurnitureId() {
        Furniture desk = desk("desk-1", 2, 2, 0);
        Furniture chair = new Furniture("chair-1", "chair", "의자", 0.5, 0.5, 0.8,
                new Position(4, 4), 0, FurnitureStatus.EXISTING);

        FeedbackExecution execution = executor.execute(direct(move("op-1",
                new FeedbackTargetSelector("", "desk", ""), FeedbackRelation.LEFT, FeedbackMagnitude.SMALL)),
                room(6, 6), List.of(desk, chair));

        assertThat(execution.result().applied()).isTrue();
        assertThat(execution.furniture().getFirst().getPosition().getX()).isEqualTo(1.8);
    }

    @Test
    void ambiguousFurnitureTypeDoesNotSelectTheFirstFurniture() {
        List<Furniture> before = List.of(desk("desk-1", 1, 1, 0), desk("desk-2", 4, 4, 0));

        FeedbackExecution execution = executor.execute(direct(move("op-1",
                new FeedbackTargetSelector("", "desk", ""), FeedbackRelation.RIGHT, FeedbackMagnitude.SMALL)),
                room(6, 6), before);

        assertThat(execution.result().applied()).isFalse();
        assertThat(execution.result().noChangeReason()).isEqualTo("AMBIGUOUS_TARGET");
        assertThat(execution.furniture()).containsExactlyElementsOf(before);
    }

    @Test
    void invalidMoveCandidateKeepsOriginalFurnitureState() {
        Room room = room(3, 4);
        Furniture prototype = desk("desk-1", 1.5, 2, 0);
        double safeMaxX = FurnitureBoundary.clamp(room, new Position(10, 2), prototype)
                .orElseThrow().getX();
        Furniture before = desk("desk-1", safeMaxX, 2, 0);

        FeedbackExecution execution = executor.execute(direct(move("op-1",
                new FeedbackTargetSelector("desk-1", "desk", ""), FeedbackRelation.RIGHT, FeedbackMagnitude.LARGE)),
                room, List.of(before));

        assertThat(execution.result().applied()).as(execution.result().noChangeReason()).isFalse();
        assertThat(execution.furniture().getFirst().getPosition().getX()).isEqualTo(safeMaxX);
    }

    @Test
    void rotateNearWallMovesFurnitureInwardByTheMinimumRequiredDistance() {
        Furniture before = desk("desk-1", 0.8, 0.38, 0);

        FeedbackExecution execution = executor.execute(direct(rotate("op-1",
                new FeedbackTargetSelector("desk-1", "desk", ""),
                FeedbackOrientation.QUARTER_TURN_CW, List.of())), room(3, 3), List.of(before));

        assertThat(execution.result().applied()).as(execution.result().noChangeReason()).isTrue();
        assertThat(execution.furniture().getFirst().getRotation()).isEqualTo(90);
        assertThat(execution.furniture().getFirst().getPosition().getZ())
                .isCloseTo(0.68, org.assertj.core.data.Offset.offset(1.0e-7));
    }

    @Test
    void moveFurnitureLargerThanRoomReportsBoundaryFailureAndKeepsSnapshot() {
        Furniture oversized = new Furniture("desk-1", "desk", "책상", 3.0, 0.6, 0.73,
                new Position(1.5, 1.5), 0, FurnitureStatus.RECOMMENDED);

        FeedbackExecution execution = executor.execute(direct(move("op-1",
                new FeedbackTargetSelector("desk-1", "desk", ""),
                FeedbackRelation.RIGHT, FeedbackMagnitude.SMALL)), room(3, 3), List.of(oversized));

        assertThat(execution.result().applied()).isFalse();
        assertThat(execution.result().noChangeReason()).isEqualTo("NO_VALID_BOUNDARY_PLACEMENT");
        assertThat(execution.furniture()).containsExactly(oversized);
    }

    @Test
    void rotateThatCannotFitReportsRotationOutOfBoundsAndKeepsSnapshot() {
        Furniture before = new Furniture("desk-1", "desk", "책상", 1.0, 2.5, 0.73,
                new Position(0.55, 1.5), 0, FurnitureStatus.RECOMMENDED);

        FeedbackExecution execution = executor.execute(direct(rotate("op-1",
                new FeedbackTargetSelector("desk-1", "desk", ""),
                FeedbackOrientation.QUARTER_TURN_CW, List.of())), room(1.1, 3), List.of(before));

        assertThat(execution.result().applied()).isFalse();
        assertThat(execution.result().noChangeReason()).isEqualTo("ROTATION_OUT_OF_BOUNDS");
        assertThat(execution.furniture()).containsExactly(before);
    }

    @Test
    void furnitureAdditionPathAllowsCanonicalRugToOverlapExistingFurniture() {
        Furniture bed = oversizedBedCoveringCandidateArea();
        FeedbackOperation addRug = add("op-1", "rug");

        FeedbackExecution execution = executor.execute(direct(addRug), room(6, 6), List.of(bed), null,
                FurnitureAdditionPolicy.MAX_NEW_ADDITIONS);

        assertThat(execution.result().applied()).isTrue();
        assertThat(execution.furniture()).anyMatch(FurnitureDomainPolicy::isRug);
        assertThat(new ValidationService().validate(room(6, 6), execution.furniture()).isCollisionFree()).isTrue();
    }

    @Test
    void naturalLanguageFeedbackPathUsesTheSameRugOverlayPolicy() {
        Furniture bed = oversizedBedCoveringCandidateArea();
        Room room = room(6, 6);
        FeedbackPlan plan = new RuleBasedFeedbackPlanInterpreter().interpret(
                "러그를 하나 추가해줘", room, List.of(bed), null);

        FeedbackExecution execution = executor.execute(plan, room, List.of(bed), null);

        assertThat(plan.operations()).extracting(FeedbackOperation::type)
                .containsExactly(FeedbackOperationType.ADD_FURNITURE);
        assertThat(execution.result().applied()).isTrue();
        assertThat(execution.furniture()).anyMatch(FurnitureDomainPolicy::isRug);
        assertThat(new ValidationService().validate(room, execution.furniture()).isCollisionFree()).isTrue();
    }

    @Test
    void naturalLanguageMoveKeepsFurnitureCountIdAndProductVariant() {
        Furniture chair = new Furniture("chair-1", "desk_chair", "기존 의자", 0.5, 0.5, 0.8,
                new Position(2, 2), 0, FurnitureStatus.EXISTING,
                "desk-chair-basic-01", List.of("minimal"), "desk-chair-basic");
        FeedbackPlan plan = new RuleBasedFeedbackPlanInterpreter().interpret(
                "의자를 오른쪽으로 배치해줘", room(6, 6), List.of(chair), null);

        FeedbackExecution execution = executor.execute(plan, room(6, 6), List.of(chair));

        assertThat(plan.operations()).singleElement().satisfies(operation ->
                assertThat(operation.type()).isEqualTo(FeedbackOperationType.MOVE));
        assertThat(execution.result().applied()).isTrue();
        assertThat(execution.furniture()).singleElement().satisfies(after -> {
            assertThat(after.getId()).isEqualTo("chair-1");
            assertThat(after.getProductId()).isEqualTo("desk-chair-basic-01");
            assertThat(after.getVariantId()).isEqualTo("desk-chair-basic");
        });
    }

    private FeedbackPlan direct(FeedbackOperation operation) {
        return new FeedbackPlan("2.0", FeedbackRequestKind.DIRECT, List.of(operation), List.of(), null,
                "test", FeedbackSource.LLM, false);
    }

    private FeedbackOperation move(String operationId, FeedbackTargetSelector target,
                                   FeedbackRelation relation, FeedbackMagnitude magnitude) {
        return new FeedbackOperation(operationId, FeedbackOperationType.MOVE, target,
                new FeedbackPlacement(relation, magnitude, null), null, List.of());
    }

    private FeedbackOperation rotate(String operationId, FeedbackTargetSelector target,
                                     FeedbackOrientation orientation, List<String> dependsOn) {
        return new FeedbackOperation(operationId, FeedbackOperationType.ROTATE, target,
                new FeedbackPlacement(null, null, orientation), null, dependsOn);
    }

    private FeedbackOperation add(String operationId, String type) {
        return new FeedbackOperation(operationId, FeedbackOperationType.ADD_FURNITURE,
                new FeedbackTargetSelector("", type, ""), null,
                new FeedbackPlacement(FeedbackRelation.NEAR_WALL, null, null, null), null,
                new FeedbackProductRequirements(type, FeedbackSizePreference.ANY, false, List.of()),
                null, List.of());
    }

    private Furniture oversizedBedCoveringCandidateArea() {
        return new Furniture("bed-1", "bed", "큰 침대", 5.0, 5.0, 0.5,
                new Position(3.0, 3.0), 0, FurnitureStatus.EXISTING);
    }

    private Furniture desk(String id, double x, double z, double rotation) {
        return new Furniture(id, "desk", "책상", 1.0, 0.6, 0.73,
                new Position(x, z), rotation, FurnitureStatus.RECOMMENDED,
                "desk-compact-01", List.of("minimal"), "desk-compact");
    }

    private Room room(double width, double depth) {
        return new Room(null, width, depth, 2.4, "meter", List.of(), List.of());
    }
}
