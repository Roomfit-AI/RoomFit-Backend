package com.roomfit.room;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/**
 * 문/창문 도메인 모델.
 * wall: north/south/east/west, offset: 해당 벽 시작점 기준 떨어진 거리(meter)
 */
@Embeddable
@Schema(description = "방의 문/창문 정보")
public class Opening {

    @Schema(description = "문/창문 ID", example = "door-1")
    private String id;
    @Schema(description = "문/창문 타입", example = "door", allowableValues = {"door", "window"})
    private String type;   // door, window
    @Schema(description = "위치한 벽", example = "south", allowableValues = {"north", "south", "east", "west"})
    private String wall;   // north, south, east, west
    // "offset"은 H2/PostgreSQL 예약어라 컬럼명으로 그대로 쓸 수 없어 opening_offset으로 매핑.
    @Column(name = "opening_offset")
    @Schema(description = "해당 벽 시작점 기준 offset(meter)", example = "0.7")
    private double offset;
    @Schema(description = "폭(meter)", example = "0.8")
    private double width;
    @Schema(description = "높이(meter)", example = "2.1")
    private double height;
    @Schema(description = "창문 하단 높이(meter). door는 null", example = "0.9", nullable = true)
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
