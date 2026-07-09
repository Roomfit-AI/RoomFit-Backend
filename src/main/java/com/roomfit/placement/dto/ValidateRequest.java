package com.roomfit.placement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "현재 화면 배치 검증 요청. 저장은 수행하지 않습니다. furniture 배열에는 현재 layoutId에 포함된 전체 furniture id 목록을 전달해야 합니다. 각 item은 full furniture object가 아니라 id, position, rotation, status 중심의 compact update item입니다. 일부 가구 id만 전달하면 FURNITURE_ARRAY_MISMATCH가 발생할 수 있습니다.")
public class ValidateRequest {

    @Schema(description = "검증 대상 배치 ID", example = "1")
    private Long layoutId;
    @Schema(description = "프론트 화면의 현재 가구 배치 전체 배열. 전체 가구 배열은 모든 furniture id를 포함한다는 뜻이며, type/label/width/depth/height/productId/styleTags 같은 추천 결과 메타데이터를 다시 보내는 뜻은 아닙니다. 일부 가구 id만 전달하면 FURNITURE_ARRAY_MISMATCH가 발생할 수 있습니다.")
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
