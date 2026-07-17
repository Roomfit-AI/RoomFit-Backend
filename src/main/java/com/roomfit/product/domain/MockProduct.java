package com.roomfit.product.domain;

import com.roomfit.common.VariantIdValidator;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class MockProduct {

    private final String productId;
    private final String variantId;
    private final String type;
    private final String name;
    private final String brand;
    private final double width;
    private final double depth;
    private final double height;
    private final Integer price;
    private final List<String> styleTags;
    private final String imageUrl;
    private final String purchaseUrl;
    private final RequiredClearance requiredClearance;

    public MockProduct(String productId, String type, String name, String brand,
                       double width, double depth, double height, int price,
                       List<String> styleTags, String imageUrl,
                       RequiredClearance requiredClearance) {
        this(productId, type, name, brand, width, depth, height, Integer.valueOf(price),
                styleTags, imageUrl, requiredClearance);
    }

    public MockProduct(String productId, String type, String name, String brand,
                       double width, double depth, double height, Integer price,
                       List<String> styleTags, String imageUrl,
                       RequiredClearance requiredClearance) {
        this(productId, null, type, name, brand, width, depth, height, price,
                styleTags, imageUrl, null, requiredClearance);
    }

    public MockProduct(String productId, String type, String name, String brand,
                       double width, double depth, double height, int price,
                       List<String> styleTags, String imageUrl, String purchaseUrl,
                       RequiredClearance requiredClearance) {
        this(productId, type, name, brand, width, depth, height, Integer.valueOf(price),
                styleTags, imageUrl, purchaseUrl, requiredClearance);
    }

    public MockProduct(String productId, String type, String name, String brand,
                       double width, double depth, double height, Integer price,
                       List<String> styleTags, String imageUrl, String purchaseUrl,
                       RequiredClearance requiredClearance) {
        this(productId, null, type, name, brand, width, depth, height, price,
                styleTags, imageUrl, purchaseUrl, requiredClearance);
    }

    public MockProduct(String productId, String variantId, String type, String name, String brand,
                       double width, double depth, double height, int price,
                       List<String> styleTags, String imageUrl, String purchaseUrl,
                       RequiredClearance requiredClearance) {
        this(productId, variantId, type, name, brand, width, depth, height, Integer.valueOf(price),
                styleTags, imageUrl, purchaseUrl, requiredClearance);
    }

    public MockProduct(String productId, String variantId, String type, String name, String brand,
                       double width, double depth, double height, Integer price,
                       List<String> styleTags, String imageUrl, String purchaseUrl,
                       RequiredClearance requiredClearance) {
        this.productId = productId;
        this.variantId = VariantIdValidator.validateNullable(variantId);
        this.type = type;
        this.name = name;
        this.brand = brand;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.price = price;
        this.styleTags = List.copyOf(styleTags);
        this.imageUrl = imageUrl;
        this.purchaseUrl = validatePurchaseUrl(purchaseUrl);
        if (requiredClearance == null) {
            throw new IllegalArgumentException("requiredClearance must not be null");
        }
        this.requiredClearance = requiredClearance;
    }

    private static String validatePurchaseUrl(String purchaseUrl) {
        if (purchaseUrl == null) {
            return null;
        }
        if (purchaseUrl.isBlank()) {
            throw new IllegalArgumentException("purchaseUrl must be null or an absolute HTTP(S) URL");
        }

        try {
            URI uri = new URI(purchaseUrl);
            String scheme = uri.getScheme();
            boolean supportedScheme = "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
            if (!supportedScheme || uri.getHost() == null || uri.getHost().isBlank()) {
                throw new IllegalArgumentException("purchaseUrl must be null or an absolute HTTP(S) URL");
            }
            return purchaseUrl;
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("purchaseUrl must be null or an absolute HTTP(S) URL", e);
        }
    }

    public String getProductId() {
        return productId;
    }

    public String getVariantId() {
        return variantId;
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

    public Integer getPrice() {
        return price;
    }

    public List<String> getStyleTags() {
        return styleTags;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getPurchaseUrl() {
        return purchaseUrl;
    }

    public RequiredClearance getRequiredClearance() {
        return requiredClearance;
    }
}
