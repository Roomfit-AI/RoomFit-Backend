package com.roomfit.placement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "검증/수정 저장 시 사용하는 가구 위치 DTO")
public class FurniturePositionDto {

    @Schema(description = "가구 ID. 기존 recommendedFurniture의 id와 일치해야 합니다.", example = "desk-rec-1")
    private String id;
    @Schema(description = "x-z 평면에서의 가구 중심 좌표")
    private PositionDto position;
    @Schema(description = "degree 단위 회전 각도", example = "0")
    private double rotation;
    @Schema(description = "가구 상태. update 요청에서 사용하며 validate 요청에서는 null 가능", example = "USER_MODIFIED", allowableValues = {"EXISTING", "DELETED", "RECOMMENDED", "USER_MODIFIED"})
    private String status; // update 요청에서만 사용 (예: "user_modified"), validate 요청에서는 null 가능

    protected FurniturePositionDto() {
        // JSON 역직렬화용
    }

    public String getId() {
        return id;
    }

    public PositionDto getPosition() {
        return position;
    }

    public double getRotation() {
        return rotation;
    }

    public String getStatus() {
        return status;
    }

    @Schema(description = "x-z 평면에서의 중심 좌표")
    public static class PositionDto {
        @Schema(description = "x축 중심 좌표(meter)", example = "2.3")
        private double x;
        @Schema(description = "z축 중심 좌표(meter)", example = "1.0")
        private double z;

        protected PositionDto() {
        }

        public double getX() {
            return x;
        }

        public double getZ() {
            return z;
        }
    }
}
