package com.roomfit.product.domain;

import java.util.List;

public class MockProduct {

    private final String productId;
    private final String type;
    private final String name;
    private final String brand;
    private final double width;
    private final double depth;
    private final double height;
    private final int price;
    private final List<String> styleTags;
    private final String imageUrl;
    private final RequiredClearance requiredClearance;

    public MockProduct(String productId, String type, String name, String brand,
                       double width, double depth, double height, int price,
                       List<String> styleTags, String imageUrl,
                       RequiredClearance requiredClearance) {
        this.productId = productId;
        this.type = type;
        this.name = name;
        this.brand = brand;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.price = price;
        this.styleTags = List.copyOf(styleTags);
        this.imageUrl = imageUrl;
        this.requiredClearance = requiredClearance;
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

    public RequiredClearance getRequiredClearance() {
        return requiredClearance;
    }
}
