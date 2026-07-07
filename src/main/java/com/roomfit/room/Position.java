package com.roomfit.room;

/**
 * 가구의 중심 좌표. x: 가로, z: 세로/깊이 (단위: meter)
 */
public class Position {

    private double x;
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
