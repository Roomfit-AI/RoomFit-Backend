package com.roomfit.placement;

import com.roomfit.room.FurnitureBoundary;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import com.roomfit.room.Wall;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FurnitureFootprintTest {

    @Test
    void from_withRightAngleRotations_usesRotationAwareDimensions() {
        FurnitureBoundary.Footprint zero = FurnitureBoundary.footprint(1.4, 0.5, 0.0);
        FurnitureBoundary.Footprint ninety = FurnitureBoundary.footprint(1.4, 0.5, 90.0);
        FurnitureBoundary.Footprint oneEighty = FurnitureBoundary.footprint(1.4, 0.5, 180.0);
        FurnitureBoundary.Footprint twoSeventy = FurnitureBoundary.footprint(1.4, 0.5, 270.0);

        assertThat(zero.effectiveWidth()).isCloseTo(1.4, within(1.0e-9));
        assertThat(zero.effectiveDepth()).isCloseTo(0.5, within(1.0e-9));
        assertThat(ninety.effectiveWidth()).isCloseTo(0.5, within(1.0e-9));
        assertThat(ninety.effectiveDepth()).isCloseTo(1.4, within(1.0e-9));
        assertThat(oneEighty.effectiveWidth()).isCloseTo(1.4, within(1.0e-9));
        assertThat(oneEighty.effectiveDepth()).isCloseTo(0.5, within(1.0e-9));
        assertThat(twoSeventy.effectiveWidth()).isCloseTo(0.5, within(1.0e-9));
        assertThat(twoSeventy.effectiveDepth()).isCloseTo(1.4, within(1.0e-9));
    }

    @Test
    void from_withRotationNearNinety_snapsToRightAngle() {
        FurnitureBoundary.Footprint footprint = FurnitureBoundary.footprint(1.4, 0.5, 89.99999);

        assertThat(footprint.effectiveWidth()).isCloseTo(0.5, within(1.0e-9));
        assertThat(footprint.effectiveDepth()).isCloseTo(1.4, within(1.0e-9));
    }

    @Test
    void from_withFortyFiveDegrees_usesAllFourRotatedCorners() {
        FurnitureBoundary.Footprint footprint = FurnitureBoundary.footprint(2.0, 1.0, 45.0);
        double expectedExtent = 3.0 / Math.sqrt(2.0);

        assertThat(footprint.effectiveWidth()).isCloseTo(expectedExtent, within(1.0e-9));
        assertThat(footprint.effectiveDepth()).isCloseTo(expectedExtent, within(1.0e-9));
        assertThat(footprint.corners()).hasSize(4);
    }

    @Test
    void commonFourByThreeRoomFixturesClampInsideForAllSupportedAngles() {
        Room room = new Room(null, 4.0, 3.0, 2.4, "meter", List.of(), List.of());
        for (double rotation : List.of(0.0, 45.0, 90.0, 135.0, 180.0, 270.0)) {
            FurnitureBoundary.Footprint footprint = FurnitureBoundary.footprint(2.0, 1.0, rotation);
            Position clamped = FurnitureBoundary.clamp(room, new Position(10, -10), footprint).orElseThrow();

            assertThat(FurnitureBoundary.isInside(room, clamped, footprint))
                    .as("rotation %s", rotation)
                    .isTrue();
            if (rotation == 0.0) {
                assertThat(clamped.getX()).isCloseTo(2.92, within(1.0e-9));
                assertThat(clamped.getZ()).isCloseTo(0.58, within(1.0e-9));
            }
        }
    }

    @Test
    void generatedBedFootprint_matchesRenderedGeometryAndWallInteriorFace() {
        Room room = new Room(null, 4.0, 3.0, 2.4, "meter", List.of(), List.of());
        FurnitureBoundary.LocalFootprint local = FurnitureBoundary.resolveLocalFootprint(
                1.57, 2.22, "bed-classic-idanaes");
        FurnitureBoundary.Footprint footprint = FurnitureBoundary.footprint(
                1.57, 2.22, 0, "bed-classic-idanaes");
        Position left = FurnitureBoundary.clamp(room, new Position(-10, 1.5), footprint).orElseThrow();

        assertThat(local.source()).isEqualTo(FurnitureBoundary.FootprintSource.VARIANT_VISUAL);
        assertThat(local.minX()).isCloseTo(-0.785000026, within(1.0e-9));
        assertThat(local.maxX()).isCloseTo(0.785000026, within(1.0e-9));
        assertThat(left.getX()).isCloseTo(0.865000026, within(1.0e-9));
        assertThat(left.getX() + footprint.minX()).isCloseTo(0.08, within(1.0e-9));
    }

    @Test
    void asymmetricVisualFootprint_preservesOffsetAcrossRotations() {
        FurnitureBoundary.LocalFootprint plant = FurnitureBoundary.resolveLocalFootprint(
                0.6005779884792202, 0.6231243531863027, "plant-corner");

        assertThat(plant.minX()).isCloseTo(-0.34691298, within(1.0e-9));
        assertThat(plant.maxX()).isCloseTo(0.329564078, within(1.0e-9));
        assertThat(plant.minZ()).isCloseTo(-0.319191822, within(1.0e-9));
        assertThat(plant.maxZ()).isCloseTo(0.315590616, within(1.0e-9));
        for (double rotation : List.of(0.0, 45.0, 90.0, 135.0)) {
            FurnitureBoundary.Footprint footprint = FurnitureBoundary.footprint(
                    0.6005779884792202, 0.6231243531863027, rotation, "plant-corner");
            assertThat(footprint.corners()).hasSize(4);
        }
    }

    @Test
    void unknownVariant_usesExplicitNominalFallback() {
        FurnitureBoundary.LocalFootprint local = FurnitureBoundary.resolveLocalFootprint(2.0, 1.0, "unknown-id");

        assertThat(local.source()).isEqualTo(FurnitureBoundary.FootprintSource.UNKNOWN_VARIANT_NOMINAL);
        assertThat(local.minX()).isEqualTo(-1.0);
        assertThat(local.maxX()).isEqualTo(1.0);
        assertThat(local.minZ()).isEqualTo(-0.5);
        assertThat(local.maxZ()).isEqualTo(0.5);
    }

    @Test
    void usableBounds_useTheRenderedWallInteriorFace() {
        Room room = new Room(null, "wall test", 4, 3, 2.4, "meter",
                List.of(new Wall("left", new Position(0.05, 0), new Position(0.05, 3), 2.4, 0.2)),
                List.of(), List.of(), com.roomfit.room.RoomSource.ROOMPLAN, null, null);

        FurnitureBoundary.UsableBounds usable = FurnitureBoundary.usableBounds(room).orElseThrow();

        assertThat(usable.minX()).isCloseTo(0.17, within(1.0e-9));
        assertThat(usable.maxX()).isCloseTo(3.98, within(1.0e-9));
        assertThat(usable.minZ()).isCloseTo(0.02, within(1.0e-9));
        assertThat(usable.maxZ()).isCloseTo(2.98, within(1.0e-9));
    }
}
