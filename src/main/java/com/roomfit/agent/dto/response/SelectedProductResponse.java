package com.roomfit.agent.dto.response;

import com.roomfit.product.domain.MockProduct;

import java.util.List;

public class SelectedProductResponse {

    private final String productId;
    private final String type;
    private final String name;
    private final double width;
    private final double depth;
    private final double height;
    private final List<String> styleTags;
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
