package com.roomfit.product.dto.response;

import com.roomfit.product.domain.MockProduct;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "MVP 제품 카드 렌더링용 Mock Product 응답")
public class MockProductResponse {

    @Schema(description = "제품 ID. Agent Context의 selectedProductIds에 사용됩니다.", example = "desk-01")
    private final String productId;
    @Schema(description = "제품 가구 타입", example = "desk")
    private final String type;
    @Schema(description = "제품명", example = "화이트 미니멀 책상")
    private final String name;
    @Schema(description = "브랜드명", example = "RoomFit Mock")
    private final String brand;
    @Schema(description = "제품 가로 길이(meter)", example = "1.2")
    private final double width;
    @Schema(description = "제품 깊이(meter)", example = "0.6")
    private final double depth;
    @Schema(description = "제품 높이(meter)", example = "0.72")
    private final double height;
    @Schema(description = "가격(원)", example = "89000")
    private final int price;
    @Schema(description = "추천/스타일 계산에 사용되는 태그")
    private final List<String> styleTags;
    @Schema(description = "제품 이미지 URL", example = "/images/products/desk-white.png")
    private final String imageUrl;
    @Schema(description = "가구 앞/옆 권장 여유 공간")
    private final RequiredClearanceResponse requiredClearance;

    private MockProductResponse(String productId, String type, String name, String brand,
                                double width, double depth, double height, int price,
                                List<String> styleTags, String imageUrl,
                                RequiredClearanceResponse requiredClearance) {
        this.productId = productId;
        this.type = type;
        this.name = name;
        this.brand = brand;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.price = price;
        this.styleTags = styleTags;
        this.imageUrl = imageUrl;
        this.requiredClearance = requiredClearance;
    }

    public static MockProductResponse from(MockProduct product) {
        return new MockProductResponse(
                product.getProductId(),
                product.getType(),
                product.getName(),
                product.getBrand(),
                product.getWidth(),
                product.getDepth(),
                product.getHeight(),
                product.getPrice(),
                product.getStyleTags(),
                product.getImageUrl(),
                RequiredClearanceResponse.from(product.getRequiredClearance())
        );
    }

    public String getProductId() {
        return productId;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getBrand() {
        return brand;
    }

    public double getWidth() {
        return width;
    }

    public double getDepth() {
        return depth;
    }

    public double getHeight() {
        return height;
    }

    public int getPrice() {
        return price;
    }

    public List<String> getStyleTags() {
        return styleTags;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public RequiredClearanceResponse getRequiredClearance() {
        return requiredClearance;
    }
}
