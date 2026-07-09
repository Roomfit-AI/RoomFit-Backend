package com.roomfit.room.dto;

import java.util.List;

public class RoomUploadRequest {

    private String name;
    private RoomData room;
    private List<OpeningData> openings;
    private List<FurnitureData> furniture;

    protected RoomUploadRequest() {
        // JSON 역직렬화용
    }

    public String getName() {
        return name;
    }

    public RoomData getRoom() {
        return room;
    }

    public List<OpeningData> getOpenings() {
        return openings;
    }

    public List<FurnitureData> getFurniture() {
        return furniture;
    }

    public static class RoomData {
        private Double width;
        private Double depth;
        private Double height;
        private String unit;

        protected RoomData() {
            // JSON 역직렬화용
        }

        public Double getWidth() {
            return width;
        }

        public Double getDepth() {
            return depth;
        }

        public Double getHeight() {
            return height;
        }

        public String getUnit() {
            return unit;
        }
    }

    public static class OpeningData {
        private String id;
        private String type;
        private String wall;
        private Double offset;
        private Double width;
        private Double height;
        private Double sillHeight;

        protected OpeningData() {
            // JSON 역직렬화용
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getWall() {
            return wall;
        }

        public Double getOffset() {
            return offset;
        }

        public Double getWidth() {
            return width;
        }

        public Double getHeight() {
            return height;
        }

        public Double getSillHeight() {
            return sillHeight;
        }
    }

    public static class FurnitureData {
        private String id;
        private String type;
        private String label;
        private Double width;
        private Double depth;
        private Double height;
        private PositionData position;
        private Double rotation;
        private String status;

        protected FurnitureData() {
            // JSON 역직렬화용
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getLabel() {
            return label;
        }

        public Double getWidth() {
            return width;
        }

        public Double getDepth() {
            return depth;
        }

        public Double getHeight() {
            return height;
        }

        public PositionData getPosition() {
            return position;
        }

        public Double getRotation() {
            return rotation;
        }

        public String getStatus() {
            return status;
        }
    }

    public static class PositionData {
        private Double x;
        private Double z;

        protected PositionData() {
            // JSON 역직렬화용
        }

        public Double getX() {
            return x;
        }

        public Double getZ() {
            return z;
        }
    }
}
