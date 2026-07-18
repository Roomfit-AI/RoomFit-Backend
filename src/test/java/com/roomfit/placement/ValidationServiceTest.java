package com.roomfit.placement;

import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureBoundary;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ValidationServiceTest {

    private final ValidationService validationService = new ValidationService();
    private final Room room = new Room(null, 3.2, 4.5, 2.4, "meter", List.of(), List.of());

    @Test
    void validate_withRotatedDeskOutsideBoundary_returnsBoundaryInvalid() {
        ValidationResult result = validationService.validate(room, List.of(rotatedDeskAt(0.4)));

        assertThat(result.isBoundaryValid()).isFalse();
    }

    @Test
    void validate_withRotatedDeskInsideBoundary_returnsBoundaryValid() {
        ValidationResult result = validationService.validate(room, List.of(rotatedDeskAt(0.78)));

        assertThat(result.isBoundaryValid()).isTrue();
    }

    @Test
    void validate_withCenterInsideButRotatedCornerOutside_returnsBoundaryInvalid() {
        Furniture diagonal = new Furniture("desk-1", "desk", "desk", 2.0, 1.0, 0.72,
                new Position(1.05, 1.05), 45.0, FurnitureStatus.EXISTING);

        assertThat(validationService.validate(room, List.of(diagonal)).isBoundaryValid()).isFalse();
    }

    @Test
    void validate_requiresWallClearance() {
        Furniture touching = new Furniture("desk-1", "desk", "desk", 1.0, 0.5, 0.72,
                new Position(0.5, 1.0), 0.0, FurnitureStatus.EXISTING);
        Furniture safe = new Furniture("desk-1", "desk", "desk", 1.0, 0.5, 0.72,
                new Position(0.5 + FurnitureBoundary.DEFAULT_WALL_THICKNESS_METERS / 2.0
                        + FurnitureBoundary.WALL_CLEARANCE_METERS, 1.0),
                0.0, FurnitureStatus.EXISTING);

        assertThat(validationService.validate(room, List.of(touching)).isBoundaryValid()).isFalse();
        assertThat(validationService.validate(room, List.of(safe)).isBoundaryValid()).isTrue();
    }

    @Test
    void validate_withFurnitureLargerThanRoom_returnsBoundaryInvalid() {
        Furniture oversized = new Furniture("bed-1", "bed", "bed", 3.2, 1.0, 0.5,
                new Position(1.6, 2.0), 0.0, FurnitureStatus.EXISTING);

        assertThat(validationService.validate(room, List.of(oversized)).isBoundaryValid()).isFalse();
    }

    @Test
    void validate_commonRotationFixtures_matchRectangularBoundaryPolicy() {
        for (double rotation : List.of(0.0, 45.0, 90.0, 135.0, 180.0, 270.0)) {
            FurnitureBoundary.Footprint footprint = FurnitureBoundary.footprint(2.0, 1.0, rotation);
            FurnitureBoundary.UsableBounds usable = FurnitureBoundary.usableBounds(room).orElseThrow();
            Position safeCorner = new Position(
                    usable.minX() - footprint.minX(),
                    usable.minZ() - footprint.minZ());
            Furniture furniture = new Furniture("fixture-" + rotation, "desk", "desk", 2.0, 1.0, 0.72,
                    safeCorner, rotation, FurnitureStatus.EXISTING);

            assertThat(validationService.validate(room, List.of(furniture)).isBoundaryValid())
                    .as("rotation %s", rotation)
                    .isTrue();
        }
    }

    @Test
    void validate_withRotationAwareFootprintsOverlapping_returnsCollision() {
        Furniture desk = new Furniture("desk-1", "desk", "desk", 1.4, 0.5, 0.72,
                new Position(1.0, 1.0), 90.0, FurnitureStatus.EXISTING);
        Furniture chair = new Furniture("chair-1", "chair", "chair", 0.2, 0.2, 0.8,
                new Position(1.0, 1.6), 0.0, FurnitureStatus.EXISTING);

        ValidationResult result = validationService.validate(room, List.of(desk, chair));

        assertThat(result.isCollisionFree()).isFalse();
    }

    private Furniture rotatedDeskAt(double z) {
        return new Furniture("desk-1", "desk", "desk", 1.4, 0.5, 0.72,
                new Position(2.7, z), 90.0, FurnitureStatus.EXISTING);
    }
}
