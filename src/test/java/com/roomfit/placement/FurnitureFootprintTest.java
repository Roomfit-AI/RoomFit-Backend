package com.roomfit.placement;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class FurnitureFootprintTest {

    @Test
    void from_withRightAngleRotations_usesRotationAwareDimensions() {
        FurnitureFootprint zero = FurnitureFootprint.from(1.4, 0.5, 0.0);
        FurnitureFootprint ninety = FurnitureFootprint.from(1.4, 0.5, 90.0);
        FurnitureFootprint oneEighty = FurnitureFootprint.from(1.4, 0.5, 180.0);
        FurnitureFootprint twoSeventy = FurnitureFootprint.from(1.4, 0.5, 270.0);

        assertThat(zero.effectiveWidth()).isEqualTo(1.4);
        assertThat(zero.effectiveDepth()).isEqualTo(0.5);
        assertThat(ninety.effectiveWidth()).isEqualTo(0.5);
        assertThat(ninety.effectiveDepth()).isEqualTo(1.4);
        assertThat(oneEighty.effectiveWidth()).isEqualTo(1.4);
        assertThat(oneEighty.effectiveDepth()).isEqualTo(0.5);
        assertThat(twoSeventy.effectiveWidth()).isEqualTo(0.5);
        assertThat(twoSeventy.effectiveDepth()).isEqualTo(1.4);
    }

    @Test
    void from_withRotationNearNinety_snapsToRightAngle() {
        FurnitureFootprint footprint = FurnitureFootprint.from(1.4, 0.5, 89.99999);

        assertThat(footprint.effectiveWidth()).isCloseTo(0.5, within(1.0e-9));
        assertThat(footprint.effectiveDepth()).isCloseTo(1.4, within(1.0e-9));
    }
}
