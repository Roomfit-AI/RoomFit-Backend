package com.roomfit.room;

import java.util.List;

public class Room {

    private Long id;
    private double width;
    private double depth;
    private double height;
    private String unit; // meter 고정
    private List<Opening> openings;
    private List<Furniture> furniture;

    protected Room() {
        // JSON 역직렬화용
    }

    public Room(Long id, double width, double depth, double height, String unit,
                List<Opening> openings, List<Furniture> furniture) {
        this.id = id;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.unit = unit;
        this.openings = openings;
        this.furniture = furniture;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getUnit() {
        return unit;
    }

    public List<Opening> getOpenings() {
        return openings;
    }

    public List<Furniture> getFurniture() {
        return furniture;
    }
}
