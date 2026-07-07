package com.roomfit.placement.dto;

import java.util.List;

public class LayoutUpdateRequest {

    private List<FurniturePositionDto> furniture;

    protected LayoutUpdateRequest() {
        // JSON 역직렬화용
    }

    public List<FurniturePositionDto> getFurniture() {
        return furniture;
    }
}
