package com.roomfit.placement;

import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FurnitureSupportPolicyTest {

    @Test
    void monitorCenteredAndContainedOnDeskIsStrictStack() {
        Furniture desk = furniture("desk", 1.2, 0.7, 0.75, 2.0, 2.0, 0);
        Furniture monitor = furniture("monitor", 0.5, 0.2, 0.35, 2.0, 2.0, 0);

        assertThat(FurnitureSupportPolicy.isStrictStack(desk, monitor)).isTrue();
        assertThat(FurnitureSupportPolicy.isStrictStack(monitor, desk)).isTrue();
    }

    @Test
    void tvCenteredAndContainedOnMediaConsoleIsStrictStackInEitherOrder() {
        Furniture console = furniture("media_console", 1.6, 0.5, 0.6, 2.0, 2.0, 90);
        Furniture tv = furniture("tv", 0.9, 0.2, 0.7, 2.0, 2.0, 90);

        assertThat(FurnitureSupportPolicy.isStrictStack(console, tv)).isTrue();
        assertThat(FurnitureSupportPolicy.isStrictStack(tv, console)).isTrue();
    }

    @Test
    void oversizedDependentIsNotStrictStack() {
        Furniture desk = furniture("desk", 1.2, 0.7, 0.75, 2.0, 2.0, 0);
        Furniture monitor = furniture("monitor", 1.3, 0.2, 0.35, 2.0, 2.0, 0);

        assertThat(FurnitureSupportPolicy.isStrictStack(desk, monitor)).isFalse();
    }

    @Test
    void rotatedAabbContainmentWithoutPolygonContainmentIsNotStrictStack() {
        Furniture desk = furniture("desk", 2.0, 1.0, 0.75, 2.0, 2.0, 45);
        Furniture monitor = furniture("monitor", 1.4, 1.4, 0.35, 2.0, 2.0, 0);

        assertThat(FurnitureSupportPolicy.isStrictStack(desk, monitor)).isFalse();
        assertThat(FurnitureSupportPolicy.isStrictStack(monitor, desk)).isFalse();
    }

    @Test
    void unsupportedTypePairIsNotStrictStack() {
        Furniture desk = furniture("desk", 1.2, 0.7, 0.75, 2.0, 2.0, 0);
        Furniture tv = furniture("tv", 0.9, 0.2, 0.7, 2.0, 2.0, 0);

        assertThat(FurnitureSupportPolicy.isStrictStack(desk, tv)).isFalse();
    }

    @Test
    void supportedTypesAreNotAStackAfterIndependentMove() {
        Furniture desk = furniture("desk", 1.2, 0.7, 0.75, 2.0, 2.0, 0);
        Furniture monitor = furniture("monitor", 0.5, 0.2, 0.35, 2.01, 2.0, 0);

        assertThat(FurnitureSupportPolicy.isStrictStack(desk, monitor)).isFalse();
    }

    @Test
    void deletedSupportPairIsNotStrictStack() {
        Furniture desk = furniture("desk", 1.2, 0.7, 0.75, 2.0, 2.0, 0);
        Furniture monitor = furniture("monitor", 0.5, 0.2, 0.35, 2.0, 2.0, 0);
        monitor.setStatus(FurnitureStatus.DELETED);

        assertThat(FurnitureSupportPolicy.isStrictStack(desk, monitor)).isFalse();
    }

    private Furniture furniture(String type, double width, double depth, double height,
                                double x, double z, double rotation) {
        return new Furniture(type + "-1", type, type, width, depth, height,
                new Position(x, z), rotation, FurnitureStatus.RECOMMENDED);
    }
}
