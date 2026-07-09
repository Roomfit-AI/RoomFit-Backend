package com.roomfit.product.domain;

public class RequiredClearance {

    private final double front;
    private final double side;

    public RequiredClearance(double front, double side) {
        this.front = front;
        this.side = side;
    }

    public double getFront() {
        return front;
    }

    public double getSide() {
        return side;
    }
}
