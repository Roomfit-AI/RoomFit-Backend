package com.roomfit.room;

import java.time.LocalDateTime;
import java.util.List;

public class Room {

    private Long id;
    private String name;
    private double width;
    private double depth;
    private double height;
    private String unit; // meter 고정
    private List<Wall> walls;
    private List<Opening> openings;
    private List<Furniture> furniture;
    private RoomSource source;
    private LocalDateTime createdAt;
    // Base64-encoded JPEG snapshot taken by the iOS app at scan completion
    // (RoomCaptureViewContainer's snapshotImage()). Stored inline alongside
    // the rest of the room JSON rather than as a separate file/object-storage
    // upload, matching this repo's existing in-memory-map storage model.
    // Null for sample rooms and any older upload predating this field.
    private String thumbnailBase64;

    protected Room() {
        // JSON 역직렬화용
    }

    public Room(Long id, double width, double depth, double height, String unit,
                List<Opening> openings, List<Furniture> furniture) {
        this(id, "Sample Room", width, depth, height, unit, List.of(), openings, furniture,
                RoomSource.SAMPLE, LocalDateTime.now(), null);
    }

    public Room(Long id, String name, double width, double depth, double height, String unit,
                List<Wall> walls, List<Opening> openings, List<Furniture> furniture, RoomSource source,
                LocalDateTime createdAt, String thumbnailBase64) {
        this.id = id;
        this.name = name;
        this.width = width;
        this.depth = depth;
        this.height = height;
        this.unit = unit;
        this.walls = walls == null ? List.of() : List.copyOf(walls);
        this.openings = openings == null ? List.of() : List.copyOf(openings);
        this.furniture = furniture == null ? List.of() : List.copyOf(furniture);
        this.source = source == null ? RoomSource.SAMPLE : source;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
        this.thumbnailBase64 = thumbnailBase64;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getUnit() {
        return unit;
    }

    public List<Wall> getWalls() {
        return walls;
    }

    public List<Opening> getOpenings() {
        return openings;
    }

    public List<Furniture> getFurniture() {
        return furniture;
    }

    public RoomSource getSource() {
        return source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getThumbnailBase64() {
        return thumbnailBase64;
    }
}
