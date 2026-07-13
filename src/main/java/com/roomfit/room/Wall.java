package com.roomfit.room;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 실제로 스캔된 벽 세그먼트. start/end는 방의 코너 원점(0..width, 0..depth) 기준 좌표.
 * 방이 완전한 직사각형이 아니거나(현관 등 미스캔 구간 포함) 일부만 스캔된 경우에도
 * 그대로 표현하기 위한 원본 형태이며, width/depth 사각형과는 별개로 취급한다.
 */
@Schema(description = "실제로 스캔된 벽 세그먼트")
public class Wall {

    @Schema(description = "벽 ID", example = "wall-1")
    private String id;
    @Schema(description = "벽 시작점(방 코너 원점 기준 meter)")
    private Position start;
    @Schema(description = "벽 끝점(방 코너 원점 기준 meter)")
    private Position end;
    @Schema(description = "벽 높이(meter)", example = "2.4")
    private double height;
    @Schema(description = "벽 두께(meter)", example = "0.12")
    private double thickness;

    protected Wall() {
        // JSON 역직렬화용
    }

    public Wall(String id, Position start, Position end, double height, double thickness) {
        this.id = id;
        this.start = start;
        this.end = end;
        this.height = height;
        this.thickness = thickness;
    }

    public String getId() {
        return id;
    }

    public Position getStart() {
        return start;
    }

    public Position getEnd() {
        return end;
    }

    public double getHeight() {
        return height;
    }

    public double getThickness() {
        return thickness;
    }
}
