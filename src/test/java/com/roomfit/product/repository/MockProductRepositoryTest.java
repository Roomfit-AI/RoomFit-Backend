package com.roomfit.product.repository;

import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.domain.RequiredClearance;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MockProductRepositoryTest {

    private static final Set<String> VARIANT_PRODUCT_IDS = Set.of(
            "desk-compact-01",
            "desk-storage-01",
            "desk-corner-01",
            "desk-midcentury-glass-01"
    );

    private static final Set<String> DESK_VARIANT_IDS = Set.of(
            "desk-compact",
            "desk-storage",
            "desk-corner",
            "desk-midcentury-glass"
    );

    @Test
    void defaultProducts_haveUniqueProductIdsAndExpectedDeskVariants() {
        MockProductRepository repository = new MockProductRepository();
        List<MockProduct> products = repository.findAll();

        assertThat(products).extracting(MockProduct::getProductId).doesNotHaveDuplicates();
        assertThat(products).extracting(MockProduct::getProductId).containsAll(VARIANT_PRODUCT_IDS);

        List<MockProduct> variantProducts = products.stream()
                .filter(product -> VARIANT_PRODUCT_IDS.contains(product.getProductId()))
                .toList();
        assertThat(variantProducts).extracting(MockProduct::getVariantId)
                .containsExactlyInAnyOrderElementsOf(DESK_VARIANT_IDS)
                .doesNotHaveDuplicates();
    }

    @Test
    void defaultProducts_preserveLegacyProductMetadata() {
        MockProductRepository repository = new MockProductRepository();
        List<LegacyProduct> legacyProducts = List.of(
                new LegacyProduct("bed-01", "bed", "화이트 싱글 침대", 1.1, 2.0, 0.45, 129000,
                        List.of("minimal", "white_tone", "relax"), "/images/products/bed-white.png", null,
                        0.5, 0.2),
                new LegacyProduct("desk-01", "desk", "화이트 미니멀 책상", 1.2, 0.6, 0.72, 89000,
                        List.of("minimal", "white_tone", "study"), "/images/products/desk-white.png",
                        "https://www.ikea.com/kr/ko/p/micke-desk-white-80354281/", 0.6, 0.2),
                new LegacyProduct("chair-01", "chair", "화이트 기본 의자", 0.45, 0.45, 0.8, 39000,
                        List.of("minimal", "white_tone", "study"), "/images/products/chair-white.png",
                        "https://www.ikea.com/kr/ko/p/hauga-chair-white-50579215/", 0.4, 0.1),
                new LegacyProduct("storage-01", "storage", "우드 수납장", 0.8, 0.4, 1.2, 79000,
                        List.of("natural", "wood_tone", "storage"), "/images/products/storage-wood.png", null,
                        0.5, 0.2),
                new LegacyProduct("rug-01", "rug", "코지 원형 러그", 1.2, 1.2, 0.02, 29000,
                        List.of("cozy", "natural", "open_space"), "/images/products/rug-cozy.png", null,
                        0.1, 0.1),
                new LegacyProduct("lamp-01", "lamp", "미니멀 스탠드 조명", 0.25, 0.25, 1.2, 29000,
                        List.of("minimal", "cozy", "study"), "/images/products/lamp-minimal.png", null,
                        0.2, 0.1)
        );

        legacyProducts.forEach(expected -> assertLegacyProduct(repository, expected));
    }

    @Test
    void findById_returnsExactDeskVariantContract() {
        MockProductRepository repository = new MockProductRepository();

        assertProduct(repository, "desk-compact-01", "desk-compact", "컴팩트 책상",
                1.2, 0.6, 0.73, List.of("minimal", "classic"),
                "https://www.ikea.com/kr/ko/p/lagkapten-adils-desk-white-s09416759/");
        assertProduct(repository, "desk-storage-01", "desk-storage", "수납 결합 책상",
                1.4, 0.62, 0.73, List.of("natural", "classic"),
                "https://www.ikea.com/kr/ko/p/micke-desk-black-brown-60354277/");
        assertProduct(repository, "desk-corner-01", "desk-corner", "코너 책상",
                1.3, 1.0, 1.42, List.of("minimal", "modern"),
                "https://www.ikea.com/kr/ko/p/micke-corner-workstation-white-20354284/");
        assertProduct(repository, "desk-midcentury-glass-01", "desk-midcentury-glass", "미드센추리 글라스 책상",
                1.75, 0.74, 0.812, List.of("midcentury", "modern"),
                "https://www.oldbonesco.com/products/denali-glass-top-desk");
    }

    @Test
    void constructor_withDuplicateProductId_rejectsSeed() {
        MockProduct duplicate = product("duplicate-id");

        assertThatThrownBy(() -> new MockProductRepository(List.of(duplicate, product("duplicate-id"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate-id");
    }

    @Test
    void constructor_acceptsMoreThanFiveProductsOfSameType() {
        List<MockProduct> desks = IntStream.rangeClosed(1, 6)
                .mapToObj(index -> product("desk-test-" + index))
                .toList();

        MockProductRepository repository = new MockProductRepository(desks);

        assertThat(repository.findAll()).hasSize(6).allMatch(product -> "desk".equals(product.getType()));
        assertThat(repository.findById("desk-test-6")).isPresent();
    }

    private void assertProduct(MockProductRepository repository, String productId, String variantId, String name,
                               double width, double depth, double height, List<String> styleTags,
                               String purchaseUrl) {
        MockProduct product = repository.findById(productId).orElseThrow();

        assertThat(product.getVariantId()).isEqualTo(variantId);
        assertThat(product.getType()).isEqualTo("desk");
        assertThat(product.getName()).isEqualTo(name);
        assertThat(product.getBrand()).isNull();
        assertThat(product.getPrice()).isNull();
        assertThat(product.getImageUrl()).isNull();
        assertThat(product.getWidth()).isEqualTo(width);
        assertThat(product.getDepth()).isEqualTo(depth);
        assertThat(product.getHeight()).isEqualTo(height);
        assertThat(product.getStyleTags()).containsExactlyElementsOf(styleTags);
        assertThat(product.getPurchaseUrl()).isEqualTo(purchaseUrl);
        assertThat(product.getRequiredClearance().getFront()).isEqualTo(0.6);
        assertThat(product.getRequiredClearance().getSide()).isEqualTo(0.2);
    }

    private MockProduct product(String productId) {
        return new MockProduct(productId, "desk", "테스트 책상", null,
                1.2, 0.6, 0.73, (Integer) null, List.of("minimal"), null,
                new RequiredClearance(0.6, 0.2));
    }

    private void assertLegacyProduct(MockProductRepository repository, LegacyProduct expected) {
        MockProduct product = repository.findById(expected.productId()).orElseThrow();

        assertThat(product.getVariantId()).isNull();
        assertThat(product.getType()).isEqualTo(expected.type());
        assertThat(product.getName()).isEqualTo(expected.name());
        assertThat(product.getBrand()).isEqualTo("RoomFit Mock");
        assertThat(product.getWidth()).isEqualTo(expected.width());
        assertThat(product.getDepth()).isEqualTo(expected.depth());
        assertThat(product.getHeight()).isEqualTo(expected.height());
        assertThat(product.getPrice()).isEqualTo(expected.price());
        assertThat(product.getStyleTags()).containsExactlyElementsOf(expected.styleTags());
        assertThat(product.getImageUrl()).isEqualTo(expected.imageUrl());
        assertThat(product.getPurchaseUrl()).isEqualTo(expected.purchaseUrl());
        assertThat(product.getRequiredClearance().getFront()).isEqualTo(expected.frontClearance());
        assertThat(product.getRequiredClearance().getSide()).isEqualTo(expected.sideClearance());
    }

    private record LegacyProduct(String productId, String type, String name,
                                 double width, double depth, double height, int price,
                                 List<String> styleTags, String imageUrl, String purchaseUrl,
                                 double frontClearance, double sideClearance) {
    }
}
