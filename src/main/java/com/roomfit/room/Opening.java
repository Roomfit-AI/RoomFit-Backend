package com.roomfit.room;

/**
 * 문/창문 도메인 모델.
 * wall: north/south/east/west, offset: 해당 벽 시작점 기준 떨어진 거리(meter)
 */
public class Opening {

    private String id;
    private String type;   // door, window
    private String wall;   // north, south, east, west
    private double offset;
    private double width;
    private double height;
    private Double sillHeight; // window 전용, door는 null

    protected Opening() {
        // JSON 역직렬화용
    }

    public Opening(String id, String type, String wall, double offset, double width, double height, Double sillHeight) {
        this.id = id;
        this.type = type;
        this.wall = wall;
        this.offset = offset;
        this.width = width;
        this.height = height;
        this.sillHeight = sillHeight;
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getWall() {
        return wall;
    }

    public double getOffset() {
        return offset;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public Double getSillHeight() {
        return sillHeight;
    }
}
