package com.roomfit.room.dto;

import com.roomfit.room.Furniture;
import com.roomfit.room.Opening;
import com.roomfit.room.Room;
import com.roomfit.room.RoomSource;

import java.time.LocalDateTime;
import java.util.List;

public class RoomResponse {

    private final Long roomId;
    private final String name;
    private final RoomDimension room;
    private final List<Opening> openings;
    private final List<Furniture> furniture;
    private final RoomSource source;
    private final LocalDateTime createdAt;

    private RoomResponse(Long roomId, String name, RoomDimension room, List<Opening> openings,
                          List<Furniture> furniture, RoomSource source, LocalDateTime createdAt) {
        this.roomId = roomId;
        this.name = name;
        this.room = room;
        this.openings = openings;
        this.furniture = furniture;
        this.source = source;
        this.createdAt = createdAt;
    }

    public static RoomResponse from(Room room) {
        RoomDimension dimension = new RoomDimension(room.getWidth(), room.getDepth(), room.getHeight(), room.getUnit());
        return new RoomResponse(room.getId(), room.getName(), dimension, room.getOpenings(),
                room.getFurniture(), room.getSource(), room.getCreatedAt());
    }

    public Long getRoomId() {
        return roomId;
    }

    public String getName() {
        return name;
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

    public RoomSource getSource() {
        return source;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
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
