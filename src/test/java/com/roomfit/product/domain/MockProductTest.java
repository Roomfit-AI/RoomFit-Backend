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
        assertThat(product.getPrice()).isEqualTo(89000);
    }

    @Test
    void constructor_withNullableMetadata_keepsNullValues() {
        MockProduct product = new MockProduct(
                "desk-compact-01",
                "desk-compact",
                "desk",
                "컴팩트 책상",
                null,
                1.2,
                0.6,
                0.73,
                (Integer) null,
                List.of("minimal", "classic"),
                null,
                "https://example.com/products/desk",
                new RequiredClearance(0.6, 0.2)
        );

        assertThat(product.getBrand()).isNull();
        assertThat(product.getPrice()).isNull();
        assertThat(product.getImageUrl()).isNull();
    }

    @Test
    void constructor_withNullRequiredClearance_rejectsProduct() {
        assertThatThrownBy(() -> new MockProduct(
                "desk-test", "desk", "테스트 책상", "RoomFit Mock",
                1.2, 0.6, 0.72, 89000, List.of("minimal"),
                "/images/products/desk-test.png", null
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requiredClearance");
    }

    @Test
    void constructor_withValidVariantId_keepsVariantId() {
        MockProduct product = createProduct("desk-midcentury-glass", null);

        assertThat(product.getVariantId()).isEqualTo("desk-midcentury-glass");
    }

    @Test
    void constructor_withoutLifestyleTags_defaultsToEmptyList() {
        MockProduct product = createProduct("desk-midcentury-glass", null);

        assertThat(product.getLifestyleTags()).isEmpty();
    }

    @Test
    void constructor_withLifestyleTags_keepsLifestyleTags() {
        MockProduct product = new MockProduct(
                "desk-storage-01",
                "desk-storage",
                "desk",
                "수납 결합 책상",
                null,
                1.4,
                0.62,
                0.73,
                (Integer) null,
                List.of("natural", "classic"),
                null,
                "https://example.com/products/desk-storage",
                new RequiredClearance(0.6, 0.2),
                List.of("WORK_STUDY", "STORAGE")
        );

        assertThat(product.getLifestyleTags()).containsExactly("WORK_STUDY", "STORAGE");
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
