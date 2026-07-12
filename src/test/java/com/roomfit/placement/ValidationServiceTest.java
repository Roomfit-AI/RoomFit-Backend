package com.roomfit.placement;

import com.roomfit.room.Furniture;
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
        ValidationResult result = validationService.validate(room, List.of(rotatedDeskAt(0.7)));

        assertThat(result.isBoundaryValid()).isTrue();
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
