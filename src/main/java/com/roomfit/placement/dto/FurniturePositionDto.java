package com.roomfit.placement.dto;

public class FurniturePositionDto {

    private String id;
    private PositionDto position;
    private double rotation;
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

    public static class PositionDto {
        private double x;
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
