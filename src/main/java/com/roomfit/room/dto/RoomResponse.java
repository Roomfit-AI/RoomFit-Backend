package com.roomfit.room.dto;

import com.roomfit.room.Furniture;
import com.roomfit.room.Opening;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import com.roomfit.room.RoomImportStatus;
import com.roomfit.room.RoomImportWarning;
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
    @Schema(description = "iOS 앱이 스캔 완료 시점에 찍은 방 미리보기 스냅샷(Base64 인코딩 JPEG). 샘플 방이거나 이 필드가 추가되기 전에 업로드된 방은 null입니다.")
    private final String thumbnailBase64;
    private final RoomImportStatus importStatus;
    private final List<ImportWarningResponse> importWarnings;

    private RoomResponse(Long roomId, String name, RoomDimension room, List<Wall> walls, List<Opening> openings,
                          List<Furniture> furniture, RoomSource source, LocalDateTime createdAt, String thumbnailBase64,
                          RoomImportStatus importStatus, List<ImportWarningResponse> importWarnings) {
        this.roomId = roomId;
        this.name = name;
        this.room = room;
        this.walls = walls;
        this.openings = openings;
        this.furniture = furniture;
        this.source = source;
        this.createdAt = createdAt;
        this.thumbnailBase64 = thumbnailBase64;
        this.importStatus = importStatus;
        this.importWarnings = importWarnings;
    }

    public static RoomResponse from(Room room) {
        RoomDimension dimension = new RoomDimension(room.getWidth(), room.getDepth(), room.getHeight(), room.getUnit());
        return new RoomResponse(room.getId(), room.getName(), dimension, room.getWalls(), room.getOpenings(),
                room.getFurniture(), room.getSource(), room.getCreatedAt(), room.getThumbnailBase64(), room.getImportStatus(),
                room.getImportWarnings().stream().map(ImportWarningResponse::from).toList());
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

    public String getThumbnailBase64() {
        return thumbnailBase64;
    }

    public RoomImportStatus getImportStatus() { return importStatus; }
    public List<ImportWarningResponse> getImportWarnings() { return importWarnings; }

    public static class ImportWarningResponse {
        private final String code;
        private final String entityId;
        private final String furnitureType;
        private final String message;
        private final Double adjustmentMeters;
        private final Position originalPosition;
        private final Position normalizedPosition;
        private final Double originalRotation;
        private final Double normalizedRotation;

        private ImportWarningResponse(String code, String entityId, String furnitureType, String message, Double adjustmentMeters,
                                      Position originalPosition, Position normalizedPosition,
                                      Double originalRotation, Double normalizedRotation) {
            this.code = code;
            this.entityId = entityId;
            this.furnitureType = furnitureType;
            this.message = message;
            this.adjustmentMeters = adjustmentMeters;
            this.originalPosition = originalPosition;
            this.normalizedPosition = normalizedPosition;
            this.originalRotation = originalRotation;
            this.normalizedRotation = normalizedRotation;
        }

        private static ImportWarningResponse from(RoomImportWarning warning) {
            Position original = warning.getOriginalX() == null ? null
                    : new Position(warning.getOriginalX(), warning.getOriginalZ());
            Position normalized = warning.getNormalizedX() == null ? null
                    : new Position(warning.getNormalizedX(), warning.getNormalizedZ());
            return new ImportWarningResponse(warning.getCode(), warning.getEntityId(), warning.getFurnitureType(), warning.getMessage(),
                    warning.getAdjustmentMeters(), original, normalized, warning.getOriginalRotation(), warning.getNormalizedRotation());
        }

        public String getCode() { return code; }
        public String getEntityId() { return entityId; }
        public String getFurnitureType() { return furnitureType; }
        public String getMessage() { return message; }
        public Double getAdjustmentMeters() { return adjustmentMeters; }
        public Position getOriginalPosition() { return originalPosition; }
        public Position getNormalizedPosition() { return normalizedPosition; }
        public Double getOriginalRotation() { return originalRotation; }
        public Double getNormalizedRotation() { return normalizedRotation; }
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
