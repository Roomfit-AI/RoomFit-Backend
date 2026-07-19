package com.roomfit.placement;

import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.domain.RequiredClearance;
import com.roomfit.product.repository.MockProductRepository;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicFurnitureCompositionExecutorTest {

    @Test
    void addSelectsCollisionFreeCandidateAndGeneratesUniqueIdWithoutMutatingInput() {
        DeterministicFeedbackExecutor executor = executor(List.of(product("lamp-01", null, "lamp", "조명",
                0.2, 0.2, 1.0, List.of("minimal"))));
        Furniture bed = furniture("bed-1", "bed", "침대", 1.0, 2.0, 0.5, 3, 3, 90);
        List<Furniture> before = List.of(bed);

        FeedbackExecution execution = executor.execute(direct(add("op-1", "lamp", "bed", FeedbackSide.RIGHT)),
                room(8, 6), before);

        assertThat(execution.result().applied()).isTrue();
        assertThat(execution.furniture()).hasSize(2);
        Furniture added = execution.furniture().getLast();
        assertThat(added.getId()).isEqualTo("lamp-feedback-1");
        assertThat(added.getProductId()).isEqualTo("lamp-01");
        assertThat(added.getPosition().getX()).isEqualTo(4.2);
        assertThat(before).containsExactly(bed);
        assertThat(bed.getPosition().getX()).isEqualTo(3);
    }

    @Test
    void allInvalidAddCandidatesKeepOriginalSnapshot() {
        DeterministicFeedbackExecutor executor = executor(List.of(product("lamp-01", null, "lamp", "조명",
                0.2, 0.2, 1.0, List.of())));
        Furniture blocker = furniture("blocker", "bed", "방 전체", 2, 2, 1, 1, 1, 0);
        List<Furniture> before = List.of(blocker);

        FeedbackExecution execution = executor.execute(direct(addInCorner("op-1", "lamp")), room(2, 2), before);

        assertThat(execution.result().applied()).isFalse();
        assertThat(execution.result().noChangeReason()).isEqualTo("NO_VALID_ADD_PLACEMENT");
        assertThat(execution.furniture()).containsExactlyElementsOf(before);
    }

    @Test
    void multipleAddsWithinOneRequestUseDistinctDeterministicIds() {
        DeterministicFeedbackExecutor executor = executor(List.of(product("lamp-01", null, "lamp", "조명",
                0.2, 0.2, 1.0, List.of())));
        FeedbackOperation first = addInCorner("op-1", "lamp");
        FeedbackOperation second = new FeedbackOperation("op-2", FeedbackOperationType.ADD_FURNITURE,
                new FeedbackTargetSelector("", "lamp", ""), null,
                new FeedbackPlacement(FeedbackRelation.IN_CORNER, null, null), null,
                requirements("lamp"), null, List.of("op-1"));
        FeedbackPlan plan = composite(first, second);

        FeedbackExecution execution = executor.execute(plan, room(6, 6), List.of());

        assertThat(execution.furniture()).extracting(Furniture::getId)
                .containsExactly("lamp-feedback-1", "lamp-feedback-2");
        assertThat(execution.result().operationsApplied()).containsExactly("ADD_FURNITURE", "ADD_FURNITURE");
    }

    @Test
    void unsupportedFrontendVariantIsNeverAdded() {
        DeterministicFeedbackExecutor executor = executor(List.of(product("future-chair-01", "future-chair",
                "desk_chair", "의자", 0.5, 0.5, 0.8, List.of())));

        FeedbackExecution execution = executor.execute(direct(addInCorner("op-1", "desk_chair")), room(6, 6), List.of());

        assertThat(execution.result().applied()).isFalse();
        assertThat(execution.result().noChangeReason()).isEqualTo("NO_RENDERABLE_PRODUCT");
        assertThat(execution.furniture()).isEmpty();
    }

    @Test
    void addValidationBudgetNeverExceedsTwentyFourCandidates() {
        CountingValidationService validation = new CountingValidationService();
        MockProductRepository repository = new MockProductRepository();
        RenderableProductCatalog catalog = new RenderableProductCatalog(repository);
        DeterministicFeedbackExecutor executor = new DeterministicFeedbackExecutor(validation, repository,
                catalog, new ScoreService(), new FeedbackPlacementCandidateGenerator());
        Furniture blocker = furniture("blocker", "bed", "방 전체", 10, 10, 1, 5, 5, 0);

        executor.execute(direct(addInCorner("op-1", "desk")), room(10, 10), List.of(blocker));

        assertThat(validation.calls).isLessThanOrEqualTo(24);
    }

    @Test
    void productionCatalogAddsRepresentativeCanonicalVariants() {
        MockProductRepository repository = new MockProductRepository();
        DeterministicFeedbackExecutor executor = new DeterministicFeedbackExecutor(
                new ValidationService(), repository);

        for (String type : List.of("nightstand", "mood_lamp", "multi_table")) {
            FeedbackExecution execution = executor.execute(
                    direct(addInCorner("op-1", type)), room(8, 8), List.of());

            assertThat(execution.result().applied()).as(type).isTrue();
            assertThat(execution.furniture()).as(type).hasSize(1);
            Furniture added = execution.furniture().getFirst();
            assertThat(added.getType()).as(type).isEqualTo(type);
            assertThat(added.getProductId()).as(type).isNotBlank();
            assertThat(added.getVariantId()).as(type).isNotBlank();
        }
    }

    @Test
    void productionCatalogSwapsSofaToTheSmallestRenderableVariant() {
        MockProductRepository repository = new MockProductRepository();
        DeterministicFeedbackExecutor executor = new DeterministicFeedbackExecutor(
                new ValidationService(), repository);
        Furniture sofa = new Furniture("sofa-1", "sofa", "기존 소파", 2.18, 0.88, 0.88,
                new Position(4, 3), 0, FurnitureStatus.EXISTING,
                "sofa-classic-ektorp-01", List.of("classic", "natural"), "sofa-classic-ektorp");
        FeedbackOperation operation = new FeedbackOperation("op-1", FeedbackOperationType.SWAP_FURNITURE,
                new FeedbackTargetSelector("", "sofa", ""), null, null, null, null,
                new FeedbackProductRequirements("sofa", FeedbackSizePreference.SMALL, false, List.of()),
                List.of());

        FeedbackExecution execution = executor.execute(direct(operation), room(8, 6), List.of(sofa));

        Furniture swapped = execution.furniture().getFirst();
        assertThat(execution.result().applied()).isTrue();
        assertThat(swapped.getId()).isEqualTo("sofa-1");
        assertThat(swapped.getType()).isEqualTo("sofa");
        assertThat(swapped.getProductId()).isEqualTo("sofa-single-01");
        assertThat(swapped.getVariantId()).isEqualTo("sofa-single");
        assertThat(swapped.getStatus()).isEqualTo(FurnitureStatus.EXISTING);
    }

    @Test
    void removeByIdActuallyRemovesItemFromArray() {
        DeterministicFeedbackExecutor executor = executor(List.of());
        List<Furniture> before = List.of(
                furniture("chair-1", "chair", "의자", 0.5, 0.5, 0.8, 1, 1, 0),
                furniture("desk-1", "desk", "책상", 1, 0.6, 0.7, 4, 4, 0)
        );

        FeedbackExecution execution = executor.execute(direct(remove("op-1",
                new FeedbackTargetSelector("chair-1", "", ""))), room(6, 6), before);

        assertThat(execution.furniture()).extracting(Furniture::getId).containsExactly("desk-1");
        assertThat(before).hasSize(2);
    }

    @Test
    void removeByUniqueTypeWorksButAmbiguousTypeDoesNotChooseFirst() {
        DeterministicFeedbackExecutor executor = executor(List.of());
        Furniture one = furniture("chair-1", "chair", "의자", 0.5, 0.5, 0.8, 1, 1, 0);
        FeedbackExecution unique = executor.execute(direct(remove("op-1",
                new FeedbackTargetSelector("", "chair", ""))), room(6, 6), List.of(one));

        List<Furniture> duplicates = List.of(one,
                furniture("chair-2", "chair", "의자", 0.5, 0.5, 0.8, 4, 4, 0));
        FeedbackExecution ambiguous = executor.execute(direct(remove("op-1",
                new FeedbackTargetSelector("", "chair", ""))), room(6, 6), duplicates);

        assertThat(unique.furniture()).isEmpty();
        assertThat(ambiguous.result().applied()).isFalse();
        assertThat(ambiguous.result().noChangeReason()).isEqualTo("AMBIGUOUS_TARGET");
        assertThat(ambiguous.furniture()).containsExactlyElementsOf(duplicates);
    }

    @Test
    void missingRemoveTargetKeepsOriginalSnapshot() {
        DeterministicFeedbackExecutor executor = executor(List.of());
        List<Furniture> before = List.of(furniture("desk-1", "desk", "책상", 1, 0.6, 0.7, 3, 3, 0));

        FeedbackExecution execution = executor.execute(direct(remove("op-1",
                new FeedbackTargetSelector("", "sofa", ""))), room(6, 6), before);

        assertThat(execution.result().noChangeReason()).isEqualTo("TARGET_NOT_FOUND");
        assertThat(execution.furniture()).containsExactlyElementsOf(before);
    }

    @Test
    void ordinalAndCenterHintsResolveDeterministicallyWithoutChoosingFirst() {
        DeterministicFeedbackExecutor executor = executor(List.of());
        Furniture left = furniture("chair-left", "chair", "의자", 0.5, 0.5, 0.8, 1, 1, 0);
        Furniture center = furniture("chair-center", "chair", "의자", 0.5, 0.5, 0.8, 3, 3, 0);
        Furniture right = furniture("chair-right", "chair", "의자", 0.5, 0.5, 0.8, 5, 5, 0);

        FeedbackExecution ordinal = executor.execute(direct(remove("op-1",
                new FeedbackTargetSelector("", "chair", "", null, 2))),
                room(6, 6), List.of(left, center, right));
        FeedbackExecution byCenter = executor.execute(direct(remove("op-1",
                new FeedbackTargetSelector("", "chair", "", FeedbackLocationHint.CENTER, null))),
                room(6, 6), List.of(left, center, right));

        assertThat(ordinal.furniture()).extracting(Furniture::getId)
                .containsExactly("chair-left", "chair-right");
        assertThat(byCenter.furniture()).extracting(Furniture::getId)
                .containsExactly("chair-left", "chair-right");
    }

    @Test
    void furnitureIdHasPriorityAndTypePlusLabelDisambiguates() {
        DeterministicFeedbackExecutor executor = executor(List.of());
        Furniture red = furniture("chair-red", "chair", "빨간 의자", 0.5, 0.5, 0.8, 1, 1, 0);
        Furniture blue = furniture("chair-blue", "chair", "파란 의자", 0.5, 0.5, 0.8, 4, 4, 0);

        FeedbackExecution byLabel = executor.execute(direct(remove("op-1",
                new FeedbackTargetSelector("", "chair", "파란"))), room(6, 6), List.of(red, blue));
        FeedbackExecution byId = executor.execute(direct(remove("op-1",
                new FeedbackTargetSelector("chair-red", "desk", "없는 라벨"))), room(6, 6), List.of(red, blue));

        assertThat(byLabel.furniture()).extracting(Furniture::getId).containsExactly("chair-red");
        assertThat(byId.furniture()).extracting(Furniture::getId).containsExactly("chair-blue");
    }

    @Test
    void ambiguousReferenceTargetDoesNotAddFurniture() {
        DeterministicFeedbackExecutor executor = executor(List.of(product("lamp-01", null, "lamp", "조명",
                0.2, 0.2, 1.0, List.of())));
        List<Furniture> before = List.of(
                furniture("bed-1", "bed", "침대", 1, 2, 0.5, 1.5, 3, 0),
                furniture("bed-2", "bed", "침대", 1, 2, 0.5, 4.5, 3, 0)
        );

        FeedbackExecution execution = executor.execute(direct(add("op-1", "lamp", "bed", null)),
                room(6, 6), before);

        assertThat(execution.result().applied()).isFalse();
        assertThat(execution.result().noChangeReason()).isEqualTo("AMBIGUOUS_REFERENCE_TARGET");
        assertThat(execution.furniture()).containsExactlyElementsOf(before);
    }

    @Test
    void removeThenMoveResolvesTargetAgainAgainstLatestSnapshot() {
        DeterministicFeedbackExecutor executor = executor(List.of());
        Furniture chair = furniture("chair-1", "chair", "의자", 0.5, 0.5, 0.8, 1, 1, 0);
        Furniture desk = furniture("desk-1", "desk", "책상", 1, 0.6, 0.7, 3, 3, 0);
        FeedbackOperation remove = remove("op-1", new FeedbackTargetSelector("chair-1", "", ""));
        FeedbackOperation move = new FeedbackOperation("op-2", FeedbackOperationType.MOVE,
                new FeedbackTargetSelector("desk-1", "", ""),
                new FeedbackPlacement(FeedbackRelation.RIGHT, FeedbackMagnitude.SMALL, null), null, List.of("op-1"));

        FeedbackExecution execution = executor.execute(composite(remove, move), room(6, 6), List.of(chair, desk));

        assertThat(execution.furniture()).hasSize(1);
        assertThat(execution.furniture().getFirst().getId()).isEqualTo("desk-1");
        assertThat(execution.furniture().getFirst().getPosition().getX()).isEqualTo(3.2);
    }

    @Test
    void removeThenAddUsesTheLatestSnapshotForValidation() {
        DeterministicFeedbackExecutor executor = executor(List.of(product("lamp-01", null, "lamp", "조명",
                0.4, 0.4, 1.0, List.of())));
        Furniture blocker = furniture("chair-1", "chair", "의자", 0.8, 0.8, 0.8, 2, 2, 0);
        FeedbackOperation remove = remove("op-1", new FeedbackTargetSelector("chair-1", "", ""));
        FeedbackOperation add = new FeedbackOperation("op-2", FeedbackOperationType.ADD_FURNITURE,
                new FeedbackTargetSelector("", "lamp", ""), null,
                new FeedbackPlacement(FeedbackRelation.CENTER, null, null), null,
                requirements("lamp"), null, List.of("op-1"));

        FeedbackExecution execution = executor.execute(composite(remove, add), room(4, 4), List.of(blocker));

        assertThat(execution.result().operationsApplied()).containsExactly("REMOVE_FURNITURE", "ADD_FURNITURE");
        assertThat(execution.furniture()).extracting(Furniture::getId).containsExactly("lamp-feedback-1");
        assertThat(execution.furniture().getFirst().getPosition().getX()).isEqualTo(2);
        assertThat(execution.furniture().getFirst().getPosition().getZ()).isEqualTo(2);
    }

    @Test
    void swapBookshelfToHangerKeepsIdAndUsesCatalogMetadataAtOriginalPosition() {
        MockProduct hanger = new MockProductRepository().findById("hanger-basic-01").orElseThrow();
        DeterministicFeedbackExecutor executor = executor(List.of(hanger));
        Furniture bookshelf = new Furniture("bookshelf-1", "bookshelf", "책장", 1.2, 0.4, 1.8,
                new Position(3, 3), 0, FurnitureStatus.EXISTING,
                "bookshelf-01", List.of("classic"), null);

        FeedbackExecution execution = executor.execute(direct(swap("op-1", "bookshelf", "hanger")),
                room(8, 6), List.of(bookshelf));

        Furniture swapped = execution.furniture().getFirst();
        assertThat(swapped.getId()).isEqualTo("bookshelf-1");
        assertThat(swapped.getType()).isEqualTo("hanger");
        assertThat(swapped.getLabel()).isEqualTo("기본 스탠드형 행거");
        assertThat(swapped.getProductId()).isEqualTo("hanger-basic-01");
        assertThat(swapped.getVariantId()).isEqualTo("hanger-basic");
        assertThat(swapped.getStyleTags()).containsExactly("minimal", "modern");
        assertThat(swapped.getWidth()).isEqualTo(1.11);
        assertThat(swapped.getDepth()).isEqualTo(0.51);
        assertThat(swapped.getHeight()).isEqualTo(1.75);
        assertThat(swapped.getPosition().getX()).isEqualTo(3);
        assertThat(swapped.getPosition().getZ()).isEqualTo(3);
        assertThat(swapped.getStatus()).isEqualTo(FurnitureStatus.EXISTING);
        assertThat(bookshelf.getType()).isEqualTo("bookshelf");
    }

    @Test
    void swapSearchesAlternativePositionWhenOriginalPositionCollides() {
        DeterministicFeedbackExecutor executor = executor(List.of(product("hanger-basic-01", "hanger-basic", "hanger", "행거",
                0.4, 0.4, 1.5, List.of())));
        Furniture bookshelf = furniture("bookshelf-1", "bookshelf", "책장", 0.8, 0.4, 1.5, 3, 3, 0);
        Furniture blocker = furniture("blocker", "chair", "의자", 0.4, 0.4, 0.8, 3, 3, 0);

        FeedbackExecution execution = executor.execute(direct(swap("op-1", "bookshelf", "hanger")),
                room(6, 6), List.of(bookshelf, blocker));

        Furniture swapped = execution.furniture().stream()
                .filter(item -> item.getId().equals("bookshelf-1")).findFirst().orElseThrow();
        assertThat(execution.result().applied()).isTrue();
        assertThat(swapped.getPosition().getX() == 3 && swapped.getPosition().getZ() == 3).isFalse();
    }

    @Test
    void allInvalidSwapCandidatesKeepOriginalFurniture() {
        DeterministicFeedbackExecutor executor = executor(List.of(product("hanger-basic-01", "hanger-basic", "hanger", "행거",
                0.4, 0.4, 1.5, List.of())));
        Furniture bookshelf = furniture("bookshelf-1", "bookshelf", "책장", 0.8, 0.4, 1.5, 3, 3, 0);
        Furniture blocker = furniture("blocker", "bed", "방 전체", 6, 6, 1, 3, 3, 0);
        List<Furniture> before = List.of(bookshelf, blocker);

        FeedbackExecution execution = executor.execute(direct(swap("op-1", "bookshelf", "hanger")),
                room(6, 6), before);

        assertThat(execution.result().applied()).isFalse();
        assertThat(execution.result().noChangeReason()).isEqualTo("NO_SAFE_SWAP_CANDIDATE");
        assertThat(execution.furniture()).containsExactlyElementsOf(before);
    }

    @Test
    void operationAfterSwapCanTargetThePreservedFurnitureId() {
        DeterministicFeedbackExecutor executor = executor(List.of(product("hanger-basic-01", "hanger-basic", "hanger", "행거",
                0.4, 0.4, 1.5, List.of())));
        Furniture bookshelf = furniture("bookshelf-1", "bookshelf", "책장", 0.8, 0.4, 1.5, 2, 2, 0);
        FeedbackOperation swap = swap("op-1", "bookshelf", "hanger");
        FeedbackOperation move = new FeedbackOperation("op-2", FeedbackOperationType.MOVE,
                new FeedbackTargetSelector("bookshelf-1", "hanger", ""),
                new FeedbackPlacement(FeedbackRelation.RIGHT, FeedbackMagnitude.SMALL, null), null, List.of("op-1"));

        FeedbackExecution execution = executor.execute(composite(swap, move), room(6, 6), List.of(bookshelf));

        assertThat(execution.result().operationsApplied()).containsExactly("SWAP_FURNITURE", "MOVE");
        assertThat(execution.furniture().getFirst().getId()).isEqualTo("bookshelf-1");
        assertThat(execution.furniture().getFirst().getType()).isEqualTo("hanger");
        assertThat(execution.furniture().getFirst().getPosition().getX()).isEqualTo(2.2);
    }

    @Test
    void failedFirstOperationStopsTheCompositeBeforeAnyDependentMutation() {
        DeterministicFeedbackExecutor executor = executor(List.of(product("lamp-01", null, "lamp", "조명",
                0.2, 0.2, 1.0, List.of())));
        FeedbackOperation missingRemove = remove("op-1", new FeedbackTargetSelector("", "sofa", ""));
        FeedbackOperation dependentAdd = new FeedbackOperation("op-2", FeedbackOperationType.ADD_FURNITURE,
                new FeedbackTargetSelector("", "lamp", ""), null,
                new FeedbackPlacement(FeedbackRelation.IN_CORNER, null, null), null,
                requirements("lamp"), null, List.of("op-1"));

        FeedbackExecution execution = executor.execute(composite(missingRemove, dependentAdd), room(6, 6), List.of());

        assertThat(execution.result().applied()).isFalse();
        assertThat(execution.operationResults()).singleElement().satisfies(result -> {
            assertThat(result.status()).isEqualTo(FeedbackOperationExecution.Status.FAILED);
            assertThat(result.reasonCode()).isEqualTo("TARGET_NOT_FOUND");
        });
    }

    @Test
    void laterFailureRollsBackAnEarlierSuccessfulOperation() {
        DeterministicFeedbackExecutor executor = executor(List.of());
        Furniture chair = furniture("chair-1", "chair", "의자", 0.5, 0.5, 0.8, 2, 2, 0);
        FeedbackOperation removeChair = remove("op-1", new FeedbackTargetSelector("chair-1", "", ""));
        FeedbackOperation removeMissingSofa = new FeedbackOperation("op-2", FeedbackOperationType.REMOVE_FURNITURE,
                new FeedbackTargetSelector("", "sofa", ""), null, null, null, null, null, List.of());

        FeedbackExecution execution = executor.execute(composite(removeChair, removeMissingSofa),
                room(6, 6), List.of(chair));

        assertThat(execution.result().applied()).isFalse();
        assertThat(execution.result().operationsApplied()).isEmpty();
        assertThat(execution.furniture()).containsExactly(chair);
        assertThat(execution.operationResults()).extracting(FeedbackOperationExecution::status)
                .containsExactly(FeedbackOperationExecution.Status.FAILED, FeedbackOperationExecution.Status.FAILED);
        assertThat(execution.operationResults()).extracting(FeedbackOperationExecution::reasonCode)
                .containsExactly("ATOMIC_ROLLBACK", "TARGET_NOT_FOUND");
    }

    private DeterministicFeedbackExecutor executor(List<MockProduct> products) {
        MockProductRepository repository = new MockProductRepository();
        return new DeterministicFeedbackExecutor(new ValidationService(), repository,
                new RenderableProductCatalog(products), new ScoreService(),
                new FeedbackPlacementCandidateGenerator());
    }

    private FeedbackOperation add(String id, String type, String referenceType, FeedbackSide side) {
        return new FeedbackOperation(id, FeedbackOperationType.ADD_FURNITURE,
                new FeedbackTargetSelector("", type, ""),
                new FeedbackTargetSelector("", referenceType, ""),
                new FeedbackPlacement(FeedbackRelation.NEXT_TO, null, null, side), null,
                requirements(type), null, List.of());
    }

    private FeedbackOperation addInCorner(String id, String type) {
        return new FeedbackOperation(id, FeedbackOperationType.ADD_FURNITURE,
                new FeedbackTargetSelector("", type, ""), null,
                new FeedbackPlacement(FeedbackRelation.IN_CORNER, null, null), null,
                requirements(type), null, List.of());
    }

    private FeedbackOperation remove(String id, FeedbackTargetSelector target) {
        return new FeedbackOperation(id, FeedbackOperationType.REMOVE_FURNITURE,
                target, null, null, null, null, null, List.of());
    }

    private FeedbackOperation swap(String id, String sourceType, String replacementType) {
        return new FeedbackOperation(id, FeedbackOperationType.SWAP_FURNITURE,
                new FeedbackTargetSelector("", sourceType, ""), null, null, null, null,
                new FeedbackProductRequirements(replacementType, FeedbackSizePreference.SIMILAR, false, List.of()),
                List.of());
    }

    private FeedbackProductRequirements requirements(String type) {
        return new FeedbackProductRequirements(type, FeedbackSizePreference.ANY, false, List.of());
    }

    private FeedbackPlan direct(FeedbackOperation operation) {
        return new FeedbackPlan("2.0", FeedbackRequestKind.DIRECT, List.of(operation), List.of(), null,
                "test", FeedbackSource.LLM, false);
    }

    private FeedbackPlan composite(FeedbackOperation... operations) {
        return new FeedbackPlan("2.0", FeedbackRequestKind.COMPOSITE, List.of(operations), List.of(), null,
                "test", FeedbackSource.LLM, false);
    }

    private MockProduct product(String id, String variantId, String type, String name,
                                double width, double depth, double height, List<String> styleTags) {
        return new MockProduct(id, variantId, type, name, null, width, depth, height,
                (Integer) null, styleTags, null, null, new RequiredClearance(0.2, 0.1));
    }

    private Furniture furniture(String id, String type, String label, double width, double depth, double height,
                                double x, double z, double rotation) {
        return new Furniture(id, type, label, width, depth, height,
                new Position(x, z), rotation, FurnitureStatus.EXISTING);
    }

    private Room room(double width, double depth) {
        return new Room(null, width, depth, 2.4, "meter", List.of(), List.of());
    }

    private static final class CountingValidationService extends ValidationService {
        private int calls;

        @Override
        public ValidationResult validate(Room room, List<Furniture> furniture) {
            calls++;
            return super.validate(room, furniture);
        }
    }
}
