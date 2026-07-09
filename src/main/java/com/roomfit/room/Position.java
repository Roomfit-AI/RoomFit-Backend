package com.roomfit.room;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 가구의 중심 좌표. x: 가로, z: 세로/깊이 (단위: meter)
 */
@Schema(description = "x-z 평면에서의 가구 중심 좌표")
public class Position {

    @Schema(description = "x축 중심 좌표(meter)", example = "2.3")
    private double x;
    @Schema(description = "z축 중심 좌표(meter)", example = "1.0")
    private double z;

    protected Position() {
        // JSON 역직렬화용
    }

    public Position(double x, double z) {
        this.x = x;
        this.z = z;
    }

    public double getX() {
        return x;
    }

    public double getZ() {
        return z;
    }
}
