package com.roomfit.room;

import java.util.List;

/**
 * 가구 도메인 모델.
 * width/depth/height: 전체 길이(full extent) 기준 — Three.js BoxGeometry 인자와 동일 기준.
 * rotation: y축 기준 회전, 단위 degree.
 */
public class Furniture {

    private String id;
    private String type;      // bed, desk, chair, storage 등
    private String label;
    private double width;
    private double depth;
    private double height;
    private Position position;
    private double rotation;  // degree
    private FurnitureStatus status;
    private String productId;
    private List<String> styleTags = List.of();

    protected Furniture() {
        // JSON 역직렬화용
    }

    public Furniture(String id, String type, String label, double width, double depth, double height,
                      Position position, double rotation, FurnitureStatus status) {
        this(id, type, label, width, depth, height, position, rotation, status, null, List.of());
    }

    public Furniture(String id, String type, String label, double width, double depth, double height,
                      Position position, double rotation, FurnitureStatus status,
                      String productId, List<String> styleTags) {
        this.id = id;
        this.type = type;
        this.label = label;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.position = position;
        this.rotation = rotation;
        this.status = status;
        this.productId = productId;
        this.styleTags = styleTags == null ? List.of() : List.copyOf(styleTags);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getLabel() {
        return label;
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

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    public FurnitureStatus getStatus() {
        return status;
    }

    public void setStatus(FurnitureStatus status) {
        this.status = status;
    }

    public String getProductId() {
        return productId;
    }

    public List<String> getStyleTags() {
        return styleTags;
    }
}
