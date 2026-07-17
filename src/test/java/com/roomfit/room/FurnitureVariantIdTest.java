package com.roomfit.room;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FurnitureVariantIdTest {

    @Test
    void legacyConstructor_keepsVariantIdNull() {
        Furniture furniture = new Furniture("desk-1", "desk", "책상", 1.2, 0.6, 0.72,
                new Position(1.0, 1.0), 0, FurnitureStatus.EXISTING);

        assertThat(furniture.getVariantId()).isNull();
    }

    @Test
    void constructor_withVariantId_keepsVariantId() {
        Furniture furniture = furnitureWithVariantId("desk-compact");

        assertThat(furniture.getVariantId()).isEqualTo("desk-compact");
    }

    @Test
    void constructor_withInvalidVariantId_rejectsVariantId() {
        assertThatThrownBy(() -> furnitureWithVariantId("desk--compact"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("variantId");
    }

    private Furniture furnitureWithVariantId(String variantId) {
        return new Furniture("desk-1", "desk", "책상", 1.2, 0.6, 0.72,
                new Position(1.0, 1.0), 0, FurnitureStatus.RECOMMENDED,
                "desk-product", List.of("minimal"), variantId);
    }
}
