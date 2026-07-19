package com.roomfit.room;

import com.roomfit.placement.ValidationResult;
import com.roomfit.placement.ValidationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The canonical Sample Room is seeded once at startup (RoomSampleDataInitializer)
 * and reused as-is by both the Rooms card thumbnail and the real 3D Editor — there
 * is no separate copy to keep in sync. This exercises the actual persisted seed
 * through the same ValidationService the placement pipeline uses, so a bad
 * position/rotation edit (e.g. the wardrobe sitting askew and off the wall) fails
 * a test instead of only being caught by eye in the Editor.
 */
@SpringBootTest
class RoomSampleDataInitializerValidationTest {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ValidationService validationService;

    @Test
    void canonicalSample_allExistingFurniture_isCollisionFreeAndInBounds() {
        Room sample = roomRepository.findBySourceOrderByIdAsc(RoomSource.SAMPLE).stream()
                .filter(RoomSampleDataInitializer::isCanonicalSample)
                .findFirst()
                .orElseThrow(() -> new AssertionError("canonical Sample Room was not seeded"));

        ValidationResult result = validationService.validate(sample, sample.getFurniture());

        assertThat(result.isCollisionFree()).isTrue();
        assertThat(result.isBoundaryValid()).isTrue();
    }

    @Test
    void canonicalSample_wardrobe_standsFlushAgainstEastWallFacingIntoRoom() {
        Room sample = roomRepository.findBySourceOrderByIdAsc(RoomSource.SAMPLE).stream()
                .filter(RoomSampleDataInitializer::isCanonicalSample)
                .findFirst()
                .orElseThrow(() -> new AssertionError("canonical Sample Room was not seeded"));

        Furniture wardrobe = sample.getFurniture().stream()
                .filter(item -> "wardrobe-1".equals(item.getId()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("wardrobe-1 missing from Sample Room"));

        // rotation 90: the door-bearing width (1.2) runs parallel to the east
        // wall instead of jutting diagonally into the room.
        assertThat(wardrobe.getRotation()).isEqualTo(90.0);
        // Back edge (center.x + depth/2 = 5.39 + 0.325 = 5.715) sits just inside
        // the room's east wall (width 5.8), reading as flush rather than the
        // old ~0.2m gap.
        assertThat(wardrobe.getPosition().getX()).isGreaterThan(5.0);
        double backEdgeX = wardrobe.getPosition().getX() + wardrobe.getDepth() / 2;
        assertThat(backEdgeX).isLessThanOrEqualTo(sample.getWidth());
        assertThat(sample.getWidth() - backEdgeX).isLessThan(0.1);
    }
}
