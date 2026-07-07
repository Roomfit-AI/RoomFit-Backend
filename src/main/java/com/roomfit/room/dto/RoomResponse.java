package com.roomfit.room.dto;

import com.roomfit.room.Furniture;
import com.roomfit.room.Opening;
import com.roomfit.room.Room;

import java.util.List;

public class RoomResponse {

    private final Long roomId;
    private final RoomDimension room;
    private final List<Opening> openings;
    private final List<Furniture> furniture;

    private RoomResponse(Long roomId, RoomDimension room, List<Opening> openings, List<Furniture> furniture) {
        this.roomId = roomId;
        this.room = room;
        this.openings = openings;
        this.furniture = furniture;
    }

    public static RoomResponse from(Room room) {
        RoomDimension dimension = new RoomDimension(room.getWidth(), room.getDepth(), room.getHeight(), room.getUnit());
        return new RoomResponse(room.getId(), dimension, room.getOpenings(), room.getFurniture());
    }

    public Long getRoomId() {
        return roomId;
    }

    public RoomDimension getRoom() {
        return room;
    }

    public List<Opening> getOpenings() {
        return openings;
    }

    public List<Furniture> getFurniture() {
        return furniture;
    }

    public static class RoomDimension {
        private final double width;
        private final double depth;
        private final double height;
        private final String unit;

        public RoomDimension(double width, double depth, double height, String unit) {
            this.width = width;
            this.depth = depth;
            this.height = height;
            this.unit = unit;
        }

        public double getWidth() {
            return width;
        }

        public double getDepth() {
            return depth;
        }

        public double getHeight() {
            return height;
        }

        public String getUnit() {
            return unit;
        }
    }
}
