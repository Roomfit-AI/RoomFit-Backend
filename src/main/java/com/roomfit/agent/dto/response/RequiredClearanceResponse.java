package com.roomfit.agent.dto.response;

import com.roomfit.product.domain.RequiredClearance;

public class RequiredClearanceResponse {

    private final double front;
    private final double side;

    private RequiredClearanceResponse(double front, double side) {
        this.front = front;
        this.side = side;
    }

    public static RequiredClearanceResponse from(RequiredClearance requiredClearance) {
        return new RequiredClearanceResponse(requiredClearance.getFront(), requiredClearance.getSide());
    }

    public double getFront() {
        return front;
    }

    public double getSide() {
        return side;
    }
}
