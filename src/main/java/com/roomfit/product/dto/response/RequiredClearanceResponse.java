package com.roomfit.product.dto.response;

import com.roomfit.product.domain.RequiredClearance;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "제품 권장 여유 공간")
public class RequiredClearanceResponse {

    @Schema(description = "전면 여유 공간(meter)", example = "0.6")
    private final double front;
    @Schema(description = "측면 여유 공간(meter)", example = "0.2")
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
