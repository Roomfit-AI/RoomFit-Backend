package com.roomfit.room;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.room.dto.FurnitureUpdateRequest;
import com.roomfit.room.dto.RoomResponse;
import com.roomfit.room.dto.RoomUploadRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public RoomResponse getRoom(Long roomId) {
        Room room = findRoomOrThrow(roomId);
        return RoomResponse.from(room);
    }

    public List<RoomResponse> getSampleRooms() {
        return roomRepository.findBySourceOrderByIdAsc(RoomSource.SAMPLE).stream()
                .map(RoomResponse::from)
                .toList();
    }

    public List<RoomResponse> getRecentUploadedRooms(int limit) {
        int normalizedLimit = Math.max(0, Math.min(limit, 100));
        if (normalizedLimit == 0) {
            return List.of();
        }
        return roomRepository.findBySourceOrderByCreatedAtDescIdDesc(RoomSource.ROOMPLAN, PageRequest.of(0, normalizedLimit)).stream()
                .map(RoomResponse::from)
                .toList();
    }

    public void deleteUploadedRoom(Long roomId) {
        Room room = findRoomOrThrow(roomId);
        if (room.getSource() != RoomSource.ROOMPLAN) {
            throw new CustomException(ErrorCode.ROOM_DELETE_NOT_ALLOWED);
        }
        roomRepository.deleteById(roomId);
    }

    public RoomResponse uploadRoom(RoomUploadRequest request) {
        validateUploadRequest(request);

        RoomUploadRequest.RoomData roomData = request.getRoom();
        String name = defaultIfBlank(request.getName(), "Uploaded Room");
        String unit = defaultIfBlank(roomData.getUnit(), "meter");
        List<Wall> walls = nullToEmpty(request.getWalls()).stream()
                .map(this::toWall)
                .toList();
        List<Opening> openings = nullToEmpty(request.getOpenings()).stream()
                .map(this::toOpening)
                .toList();
        List<Furniture> furniture = nullToEmpty(request.getFurniture()).stream()
                .map(this::toFurniture)
                .toList();

        Room room = new Room(null, name, roomData.getWidth(), roomData.getDepth(), roomData.getHeight(),
                unit, walls, openings, furniture, RoomSource.ROOMPLAN, LocalDateTime.now(),
                request.getThumbnailBase64());
        validateFurnitureWithinRoom(room);

        return RoomResponse.from(roomRepository.save(room));
    }

    // manage-furniture 단계(아직 Layout이 생성되기 전)의 가구 추가/이동/삭제/
    // 회전을 통째로 반영한다. 기존 PUT /{roomId}/furniture는 상태 변경만
    // 다루므로(그 문서에 명시된 경계를 지키기 위해) 별도 엔드포인트로 추가함 —
    // uploadRoom과 동일한 검증(toFurniture, validateFurnitureWithinRoom)을 재사용.
    public RoomResponse replaceFurniture(Long roomId, List<RoomUploadRequest.FurnitureData> furnitureData) {
        Room room = findRoomOrThrow(roomId);
        List<Furniture> furniture = nullToEmpty(furnitureData).stream()
                .map(this::toFurniture)
                .toList();

        room.setFurniture(furniture);
        validateFurnitureWithinRoom(room);
        roomRepository.save(room);
        return RoomResponse.from(room);
    }

    public RoomResponse updateFurnitureStatus(Long roomId, FurnitureUpdateRequest request) {
        Room room = findRoomOrThrow(roomId);
        validateFurnitureIds(room, request);

        Map<String, String> statusById = request.getFurnitureUpdates().stream()
                .collect(Collectors.toMap(FurnitureUpdateRequest.Item::getId, FurnitureUpdateRequest.Item::getStatus));

        room.getFurniture().forEach(furniture -> {
            String rawStatus = statusById.get(furniture.getId());
            if (rawStatus == null) {
                return;
            }
            furniture.setStatus(parseStatus(rawStatus));
        });

        roomRepository.save(room);
        return RoomResponse.from(room);
    }

    private Room findRoomOrThrow(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }

    private void validateUploadRequest(RoomUploadRequest request) {
        if (request == null || request.getRoom() == null) {
            throw new CustomException(ErrorCode.INVALID_ROOM_DIMENSION);
        }

        RoomUploadRequest.RoomData room = request.getRoom();
        if (!positive(room.getWidth()) || !positive(room.getDepth()) || !positive(room.getHeight())) {
            throw new CustomException(ErrorCode.INVALID_ROOM_DIMENSION);
        }
    }

    private Wall toWall(RoomUploadRequest.WallData wall) {
        if (wall == null || isBlank(wall.getId()) || wall.getStart() == null || wall.getEnd() == null
                || wall.getStart().getX() == null || wall.getStart().getZ() == null
                || wall.getEnd().getX() == null || wall.getEnd().getZ() == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }

        double height = wall.getHeight() == null ? 0 : wall.getHeight();
        double thickness = wall.getThickness() == null ? 0 : wall.getThickness();

        return new Wall(wall.getId(),
                new Position(wall.getStart().getX(), wall.getStart().getZ()),
                new Position(wall.getEnd().getX(), wall.getEnd().getZ()),
                height, thickness);
    }

    private Opening toOpening(RoomUploadRequest.OpeningData opening) {
        if (opening == null || isBlank(opening.getId()) || isBlank(opening.getType()) || isBlank(opening.getWall())
                || !nonNegative(opening.getOffset()) || !positive(opening.getWidth()) || !positive(opening.getHeight())
                || !Set.of("door", "window").contains(opening.getType())
                || !Set.of("north", "south", "east", "west").contains(opening.getWall())) {
            throw new CustomException(ErrorCode.INVALID_OPENING_DATA);
        }

        return new Opening(opening.getId(), opening.getType(), opening.getWall(), opening.getOffset(),
                opening.getWidth(), opening.getHeight(), opening.getSillHeight());
    }

    private Furniture toFurniture(RoomUploadRequest.FurnitureData furniture) {
        if (furniture == null || isBlank(furniture.getId()) || isBlank(furniture.getType())
                || !positive(furniture.getWidth()) || !positive(furniture.getDepth()) || !positive(furniture.getHeight())
                || furniture.getPosition() == null || furniture.getPosition().getX() == null
                || furniture.getPosition().getZ() == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }

        FurnitureStatus status = parseStatus(defaultIfBlank(furniture.getStatus(), FurnitureStatus.EXISTING.name()));
        double rotation = RotationUtils.snapToRightAngle(furniture.getRotation() == null ? 0 : furniture.getRotation());
        String label = defaultIfBlank(furniture.getLabel(), furniture.getType());

        return new Furniture(furniture.getId(), furniture.getType(), label, furniture.getWidth(),
                furniture.getDepth(), furniture.getHeight(),
                new Position(furniture.getPosition().getX(), furniture.getPosition().getZ()),
                rotation, status);
    }

    private void validateFurnitureWithinRoom(Room room) {
        for (Furniture furniture : room.getFurniture()) {
            if (!FurnitureBoundary.isInside(room, furniture)) {
                throw new CustomException(ErrorCode.INVALID_FURNITURE_POSITION);
            }
        }
    }

    private FurnitureStatus parseStatus(String rawStatus) {
        try {
            return FurnitureStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_FURNITURE_STATUS);
        }
    }

    private void validateFurnitureIds(Room room, FurnitureUpdateRequest request) {
        if (request == null || request.getFurnitureUpdates() == null || request.getFurnitureUpdates().isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        boolean hasInvalidItem = request.getFurnitureUpdates().stream()
                .anyMatch(item -> item == null || isBlank(item.getId()) || isBlank(item.getStatus()));
        if (hasInvalidItem) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }

        Set<String> roomFurnitureIds = room.getFurniture().stream()
                .map(Furniture::getId)
                .collect(Collectors.toSet());

        boolean hasUnknownId = request.getFurnitureUpdates().stream()
                .map(FurnitureUpdateRequest.Item::getId)
                .anyMatch(id -> !roomFurnitureIds.contains(id));

        if (hasUnknownId) {
            throw new CustomException(ErrorCode.FURNITURE_NOT_FOUND);
        }
    }

    private <T> List<T> nullToEmpty(List<T> values) {
        return values == null ? List.of() : values;
    }

    private boolean positive(Double value) {
        return value != null && value > 0;
    }

    private boolean nonNegative(Double value) {
        return value != null && value >= 0;
    }

    private String defaultIfBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
