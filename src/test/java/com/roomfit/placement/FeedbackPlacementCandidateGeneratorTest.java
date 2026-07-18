package com.roomfit.placement;

import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.domain.RequiredClearance;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureBoundary;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.Offset.offset;

class FeedbackPlacementCandidateGeneratorTest {

    private final FeedbackPlacementCandidateGenerator generator = new FeedbackPlacementCandidateGenerator();

    @Test
    void leftAndRightCandidatesUseRotationAwareReferenceFootprint() {
        Room room = new Room(null, 8, 6, 2.4, "meter", List.of(), List.of());
        Furniture rotatedBed = new Furniture("bed-1", "bed", "침대", 1.0, 2.0, 0.5,
                new Position(3, 3), 90, FurnitureStatus.EXISTING);
        MockProduct lamp = product("lamp-01", "lamp", 0.2, 0.2, 1.0);

        List<FeedbackPlacementCandidateGenerator.PlacementCandidate> left = generator.forAdd(
                room, lamp, placement(FeedbackSide.LEFT), rotatedBed);
        List<FeedbackPlacementCandidateGenerator.PlacementCandidate> right = generator.forAdd(
                room, lamp, placement(FeedbackSide.RIGHT), rotatedBed);

        assertThat(left.getFirst().position().getX()).isCloseTo(1.8, offset(1.0e-9));
        assertThat(right.getFirst().position().getX()).isCloseTo(4.2, offset(1.0e-9));
        assertThat(left.getFirst().position().getZ()).isEqualTo(3);
        assertThat(right.getFirst().position().getZ()).isEqualTo(3);
    }

    @Test
    void cornerCandidatesKeepWallClearance() {
        Room room = new Room(null, 4, 3, 2.4, "meter", List.of(), List.of());
        MockProduct table = product("table-01", "multi_table", 1.0, 0.6, 0.7);

        List<FeedbackPlacementCandidateGenerator.PlacementCandidate> candidates = generator.forAdd(
                room, table, new FeedbackPlacement(FeedbackRelation.IN_CORNER, null, null), null);

        assertThat(candidates).isNotEmpty();
        assertThat(candidates.getFirst().position().getX())
                .isCloseTo(0.5 + FurnitureBoundary.DEFAULT_WALL_THICKNESS_METERS / 2.0
                        + FurnitureBoundary.WALL_CLEARANCE_METERS, offset(1.0e-9));
        assertThat(candidates.getFirst().position().getZ())
                .isCloseTo(0.3 + FurnitureBoundary.DEFAULT_WALL_THICKNESS_METERS / 2.0
                        + FurnitureBoundary.WALL_CLEARANCE_METERS, offset(1.0e-9));
    }

    @Test
    void oversizedProductProducesNoBoundaryCandidate() {
        Room room = new Room(null, 1, 1, 2.4, "meter", List.of(), List.of());
        MockProduct table = product("table-01", "multi_table", 1.0, 1.0, 0.7);

        assertThat(generator.forAdd(room, table,
                new FeedbackPlacement(FeedbackRelation.IN_CORNER, null, null), null)).isEmpty();
    }

    @Test
    void addAndSwapCandidates_useGeneratedVisualFootprintOffsets() {
        Room room = new Room(null, 3, 3, 2.4, "meter", List.of(), List.of());
        MockProduct plant = new MockProduct(
                "plant-corner-01", "plant-corner", "plant", "코너 식물", null,
                0.6005779884792202, 0.6231243531863027, 0.8999999922374311, (Integer) null,
                List.of("natural"), null, "https://example.com/plant",
                new RequiredClearance(0.2, 0.1));
        Furniture current = new Furniture("plant-1", "plant", "식물", 0.3, 0.3, 0.8,
                new Position(0.2, 0.2), 0, FurnitureStatus.EXISTING);

        FeedbackPlacementCandidateGenerator.PlacementCandidate added = generator.forAdd(
                room, plant, new FeedbackPlacement(FeedbackRelation.IN_CORNER, null, null), null).getFirst();
        FeedbackPlacementCandidateGenerator.PlacementCandidate swapped = generator.forSwap(
                room, current, plant).getFirst();
        FurnitureBoundary.Footprint footprint = FurnitureBoundary.footprint(
                plant.getWidth(), plant.getDepth(), added.rotation(), plant.getVariantId());

        assertThat(added.position().getX() + footprint.minX()).isCloseTo(0.08, offset(1.0e-9));
        assertThat(added.position().getZ() + footprint.minZ()).isCloseTo(0.08, offset(1.0e-9));
        assertThat(FurnitureBoundary.isInside(room, added.position(), footprint)).isTrue();
        assertThat(FurnitureBoundary.isInside(room, swapped.position(), FurnitureBoundary.footprint(
                plant.getWidth(), plant.getDepth(), swapped.rotation(), plant.getVariantId()))).isTrue();
    }

    private FeedbackPlacement placement(FeedbackSide side) {
        return new FeedbackPlacement(FeedbackRelation.NEXT_TO, null, null, side);
    }

    private MockProduct product(String id, String type, double width, double depth, double height) {
        return new MockProduct(id, type, id, null, width, depth, height, (Integer) null,
                List.of(), null, new RequiredClearance(0.1, 0.1));
    }
}
