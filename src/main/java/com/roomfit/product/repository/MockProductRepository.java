package com.roomfit.product.repository;

import com.roomfit.product.domain.MockProduct;
import com.roomfit.product.domain.RequiredClearance;
import org.springframework.stereotype.Repository;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public class MockProductRepository {

    private final List<MockProduct> products;

    public MockProductRepository() {
        this(defaultProducts());
    }

    MockProductRepository(List<MockProduct> products) {
        this.products = List.copyOf(products);
        validateUniqueProductIds(this.products);
    }

    private static List<MockProduct> defaultProducts() {
        return List.of(
            new MockProduct(
                    "bed-01",
                    "bed",
                    "화이트 싱글 침대",
                    "RoomFit Mock",
                    1.1,
                    2.0,
                    0.45,
                    129000,
                    List.of("minimal", "white_tone", "relax"),
                    "/images/products/bed-white.png",
                    new RequiredClearance(0.5, 0.2)
            ),
            new MockProduct(
                    "desk-01",
                    "desk",
                    "화이트 미니멀 책상",
                    "RoomFit Mock",
                    1.2,
                    0.6,
                    0.72,
                    89000,
                    List.of("minimal", "white_tone", "study"),
                    "/images/products/desk-white.png",
                    "https://www.ikea.com/kr/ko/p/micke-desk-white-80354281/",
                    new RequiredClearance(0.6, 0.2)
            ),
            new MockProduct(
                    "chair-01",
                    "chair",
                    "화이트 기본 의자",
                    "RoomFit Mock",
                    0.45,
                    0.45,
                    0.8,
                    39000,
                    List.of("minimal", "white_tone", "study"),
                    "/images/products/chair-white.png",
                    "https://www.ikea.com/kr/ko/p/hauga-chair-white-50579215/",
                    new RequiredClearance(0.4, 0.1)
            ),
            new MockProduct(
                    "storage-01",
                    "storage",
                    "우드 수납장",
                    "RoomFit Mock",
                    0.8,
                    0.4,
                    1.2,
                    79000,
                    List.of("natural", "wood_tone", "storage"),
                    "/images/products/storage-wood.png",
                    new RequiredClearance(0.5, 0.2)
            ),
            new MockProduct(
                    "rug-01",
                    "rug",
                    "코지 원형 러그",
                    "RoomFit Mock",
                    1.2,
                    1.2,
                    0.02,
                    29000,
                    List.of("cozy", "natural", "open_space"),
                    "/images/products/rug-cozy.png",
                    new RequiredClearance(0.1, 0.1)
            ),
            new MockProduct(
                    "lamp-01",
                    "lamp",
                    "미니멀 스탠드 조명",
                    "RoomFit Mock",
                    0.25,
                    0.25,
                    1.2,
                    29000,
                    List.of("minimal", "cozy", "study"),
                    "/images/products/lamp-minimal.png",
                    new RequiredClearance(0.2, 0.1)
            ),
            new MockProduct(
                    "desk-compact-01",
                    "desk-compact",
                    "desk",
                    "컴팩트 책상",
                    null,
                    1.2,
                    0.6,
                    0.73,
                    null,
                    List.of("minimal", "classic"),
                    null,
                    "https://www.ikea.com/kr/ko/p/lagkapten-adils-desk-white-s09416759/",
                    new RequiredClearance(0.6, 0.2)
            ),
            new MockProduct(
                    "desk-storage-01",
                    "desk-storage",
                    "desk",
                    "수납 결합 책상",
                    null,
                    1.4,
                    0.62,
                    0.73,
                    null,
                    List.of("natural", "classic"),
                    null,
                    "https://www.ikea.com/kr/ko/p/micke-desk-black-brown-60354277/",
                    new RequiredClearance(0.6, 0.2)
            ),
            new MockProduct(
                    "desk-corner-01",
                    "desk-corner",
                    "desk",
                    "코너 책상",
                    null,
                    1.3,
                    1.0,
                    1.42,
                    null,
                    List.of("minimal", "modern"),
                    null,
                    "https://www.ikea.com/kr/ko/p/micke-corner-workstation-white-20354284/",
                    new RequiredClearance(0.6, 0.2)
            ),
            new MockProduct(
                    "desk-midcentury-glass-01",
                    "desk-midcentury-glass",
                    "desk",
                    "미드센추리 글라스 책상",
                    null,
                    1.75,
                    0.74,
                    0.812,
                    null,
                    List.of("midcentury", "modern"),
                    null,
                    "https://www.oldbonesco.com/products/denali-glass-top-desk",
                    new RequiredClearance(0.6, 0.2)
            )
        );
    }

    private static void validateUniqueProductIds(List<MockProduct> products) {
        Set<String> productIds = new HashSet<>();
        for (MockProduct product : products) {
            if (!productIds.add(product.getProductId())) {
                throw new IllegalArgumentException("Duplicate productId: " + product.getProductId());
            }
        }
    }

    public List<MockProduct> findAll() {
        return products;
    }

    public Optional<MockProduct> findById(String productId) {
        return products.stream()
                .filter(product -> product.getProductId().equals(productId))
                .findFirst();
    }
}
