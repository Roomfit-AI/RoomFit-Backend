package com.roomfit.placement.dto;

import java.util.List;

public class ValidateRequest {

    private Long layoutId;
    private List<FurniturePositionDto> furniture;

    protected ValidateRequest() {
        // JSON 역직렬화용
    }

    public Long getLayoutId() {
        return layoutId;
    }

    public List<FurniturePositionDto> getFurniture() {
        return furniture;
    }
}
