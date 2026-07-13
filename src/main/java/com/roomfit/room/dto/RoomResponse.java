package com.roomfit.room.dto;

import com.roomfit.room.Furniture;
import com.roomfit.room.Opening;
import com.roomfit.room.Room;
import com.roomfit.room.RoomSource;
import com.roomfit.room.Wall;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "방 조회/업로드 응답. 프론트는 room/walls/openings/furniture를 기준으로 Three.js 방을 렌더링합니다.")
public class RoomResponse {

    @Schema(description = "방 ID", example = "1")
    private final Long roomId;
    @Schema(description = "백엔드가 부여하는 방 표시 이름. 프론트는 name을 고정 문자열로 가정하지 말고 응답값을 그대로 표시해야 합니다.", example = "RoomPlan Scan Room")
    private final String name;
    @Schema(description = "방 크기 정보. 모든 단위는 meter입니다.")
    private final RoomDimension room;
    @Schema(description = "실제로 스캔된 벽 세그먼트 목록. 비어 있으면 프론트는 width/depth 사각형으로 대체해야 합니다.")
    private final List<Wall> walls;
    @Schema(description = "문/창문 목록")
    private final List<Opening> openings;
    @Schema(description = "기존/추천/수정 가구 목록")
    private final List<Furniture> furniture;
    @Schema(description = "방 데이터 출처", example = "SAMPLE")
    private final RoomSource source;
    @Schema(description = "방 생성 시각", example = "2026-07-09T02:13:15.411289")
    private final LocalDateTime createdAt;

    private RoomResponse(Long roomId, String name, RoomDimension room, List<Wall> walls, List<Opening> openings,
                          List<Furniture> furniture, RoomSource source, LocalDateTime createdAt) {
        this.roomId = roomId;
        this.name = name;
        this.room = room;
        this.walls = walls;
        this.openings = openings;
        this.furniture = furniture;
        this.source = source;
        this.createdAt = createdAt;
    }

    public static RoomResponse from(Room room) {
        RoomDimension dimension = new RoomDimension(room.getWidth(), room.getDepth(), room.getHeight(), room.getUnit());
        return new RoomResponse(room.getId(), room.getName(), dimension, room.getWalls(), room.getOpenings(),
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

    public List<Wall> getWalls() {
        return walls;
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

    @Schema(description = "방 크기. width/depth/height는 meter 단위입니다.")
    public static class RoomDimension {
        @Schema(description = "방 가로 길이(meter)", example = "3.2")
        private final double width;
        @Schema(description = "방 깊이(meter)", example = "4.5")
        private final double depth;
        @Schema(description = "방 높이(meter)", example = "2.4")
        private final double height;
        @Schema(description = "단위. MVP에서는 meter를 사용합니다.", example = "meter")
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
