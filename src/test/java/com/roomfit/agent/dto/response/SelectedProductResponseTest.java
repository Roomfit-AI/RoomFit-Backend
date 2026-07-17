package com.roomfit.agent.dto.response;

import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.domain.RequiredClearance;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SelectedProductResponseTest {

    @Test
    void from_preservesVariantId() {
        MockProduct product = new MockProduct("desk-product", "desk-storage", "desk", "수납 결합 책상",
                "RoomFit Mock", 1.4, 0.62, 0.73, 89000, List.of("natural"),
                "/images/products/desk.png", null, new RequiredClearance(0.6, 0.2));

        SelectedProductResponse response = SelectedProductResponse.from(product);

        assertThat(response.getProductId()).isEqualTo("desk-product");
        assertThat(response.getVariantId()).isEqualTo("desk-storage");
        assertThat(response.getWidth()).isEqualTo(1.4);
        assertThat(response.getStyleTags()).containsExactly("natural");
    }
}
