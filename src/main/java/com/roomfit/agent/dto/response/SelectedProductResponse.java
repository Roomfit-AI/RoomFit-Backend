package com.roomfit.agent.dto.response;

import com.roomfit.product.domain.MockProduct;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Agent Context 응답에 포함되는 선택 제품 요약")
public class SelectedProductResponse {

    @Schema(description = "선택 제품 ID", example = "desk-01")
    private final String productId;
    @Schema(description = "제품 가구 타입", example = "desk")
    private final String type;
    @Schema(description = "제품명", example = "화이트 미니멀 책상")
    private final String name;
    @Schema(description = "제품 가로 길이(meter)", example = "1.2")
    private final double width;
    @Schema(description = "제품 깊이(meter)", example = "0.6")
    private final double depth;
    @Schema(description = "제품 높이(meter)", example = "0.72")
    private final double height;
    @Schema(description = "추천/스타일 계산에 사용되는 태그", example = "[\"minimal\", \"white_tone\", \"study\"]")
    private final List<String> styleTags;
    @Schema(description = "권장 여유 공간")
    private final RequiredClearanceResponse requiredClearance;

    private SelectedProductResponse(String productId, String type, String name,
                                    double width, double depth, double height,
                                    List<String> styleTags,
                                    RequiredClearanceResponse requiredClearance) {
        this.productId = productId;
        this.type = type;
        this.name = name;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.styleTags = styleTags;
        this.requiredClearance = requiredClearance;
    }

    public static SelectedProductResponse from(MockProduct product) {
        return new SelectedProductResponse(
                product.getProductId(),
                product.getType(),
                product.getName(),
                product.getWidth(),
                product.getDepth(),
                product.getHeight(),
                product.getStyleTags(),
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

    public double getWidth() {
        return width;
    }

    public double getDepth() {
        return depth;
    }

    public double getHeight() {
        return height;
    }

    public List<String> getStyleTags() {
        return styleTags;
    }

    public RequiredClearanceResponse getRequiredClearance() {
        return requiredClearance;
    }
}
