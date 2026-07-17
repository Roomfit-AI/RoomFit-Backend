package com.roomfit.product.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockProductTest {

    @Test
    void constructor_withoutPurchaseUrl_keepsItNull() {
        MockProduct product = new MockProduct(
                "desk-test",
                "desk",
                "테스트 책상",
                "RoomFit Mock",
                1.2,
                0.6,
                0.72,
                89000,
                List.of("minimal"),
                "/images/products/desk-test.png",
                new RequiredClearance(0.6, 0.2)
        );

        assertThat(product.getPurchaseUrl()).isNull();
        assertThat(product.getVariantId()).isNull();
    }

    @Test
    void constructor_withValidVariantId_keepsVariantId() {
        MockProduct product = createProduct("desk-midcentury-glass", null);

        assertThat(product.getVariantId()).isEqualTo("desk-midcentury-glass");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            " ",
            "Desk-compact",
            "desk_compact",
            "-desk-compact",
            "desk-compact-",
            "desk--compact"
    })
    void constructor_withInvalidVariantId_rejectsVariantId(String variantId) {
        assertThatThrownBy(() -> createProduct(variantId, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("variantId");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com/products/desk",
            "https://example.com/products/desk"
    })
    void constructor_withHttpOrHttpsPurchaseUrl_acceptsUrl(String purchaseUrl) {
        MockProduct product = createProduct(purchaseUrl);

        assertThat(product.getPurchaseUrl()).isEqualTo(purchaseUrl);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "",
            "ftp://example.com/products/desk",
            "/products/desk",
            "not a url",
            "https://"
    })
    void constructor_withInvalidPurchaseUrl_rejectsUrl(String purchaseUrl) {
        assertThatThrownBy(() -> createProduct(purchaseUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("purchaseUrl");
    }

    private MockProduct createProduct(String purchaseUrl) {
        return new MockProduct(
                "desk-test",
                "desk",
                "테스트 책상",
                "RoomFit Mock",
                1.2,
                0.6,
                0.72,
                89000,
                List.of("minimal"),
                "/images/products/desk-test.png",
                purchaseUrl,
                new RequiredClearance(0.6, 0.2)
        );
    }

    private MockProduct createProduct(String variantId, String purchaseUrl) {
        return new MockProduct(
                "desk-test",
                variantId,
                "desk",
                "테스트 책상",
                "RoomFit Mock",
                1.2,
                0.6,
                0.72,
                89000,
                List.of("minimal"),
                "/images/products/desk-test.png",
                purchaseUrl,
                new RequiredClearance(0.6, 0.2)
        );
    }
}
