package com.roomfit.placement;

import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.domain.RequiredClearance;
import com.roomfit.room.Furniture;
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

    private FeedbackPlacement placement(FeedbackSide side) {
        return new FeedbackPlacement(FeedbackRelation.NEXT_TO, null, null, side);
    }

    private MockProduct product(String id, String type, double width, double depth, double height) {
        return new MockProduct(id, type, id, null, width, depth, height, (Integer) null,
                List.of(), null, new RequiredClearance(0.1, 0.1));
    }
}
