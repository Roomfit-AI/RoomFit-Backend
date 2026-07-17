package com.roomfit.placement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "수정 배치 저장 요청. 저장 후 validationResult와 scoreSummary를 다시 계산합니다. furniture 배열에는 현재 layout의 전체 furniture id 목록을 compact update item 형태로 전달합니다.")
public class LayoutUpdateRequest {

    @Schema(description = "저장할 최종 가구 배치 전체 배열. 각 item은 full furniture object가 아니라 id, position, rotation, status 중심의 update item입니다. width/depth/height/productId/variantId/styleTags 등은 백엔드 추천 결과 메타데이터이며 요청에서 다시 전달하지 않습니다.")
    private List<FurniturePositionDto> furniture;

    protected LayoutUpdateRequest() {
        // JSON 역직렬화용
    }

    public List<FurniturePositionDto> getFurniture() {
        return furniture;
    }
}
