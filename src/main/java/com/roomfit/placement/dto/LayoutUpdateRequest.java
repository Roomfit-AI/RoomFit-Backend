package com.roomfit.placement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "수정 배치 저장 요청. 저장 후 validationResult와 scoreSummary를 다시 계산합니다.")
public class LayoutUpdateRequest {

    @Schema(description = "저장할 최종 가구 배치 전체 배열")
    private List<FurniturePositionDto> furniture;

    protected LayoutUpdateRequest() {
        // JSON 역직렬화용
    }

    public List<FurniturePositionDto> getFurniture() {
        return furniture;
    }
}
