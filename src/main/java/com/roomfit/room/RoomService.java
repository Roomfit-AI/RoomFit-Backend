package com.roomfit.room;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.room.dto.FurnitureUpdateRequest;
import com.roomfit.room.dto.RoomResponse;
import com.roomfit.room.dto.RoomUploadRequest;
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
        return roomRepository.findBySource(RoomSource.SAMPLE).stream()
                .map(RoomResponse::from)
                .toList();
    }

    public List<RoomResponse> getRecentUploadedRooms(int limit) {
        int normalizedLimit = Math.max(0, Math.min(limit, 100));
        return roomRepository.findRecentBySource(RoomSource.ROOMPLAN, normalizedLimit).stream()
                .map(RoomResponse::from)
                .toList();
    }

    public RoomResponse uploadRoom(RoomUploadRequest request) {
        validateUploadRequest(request);

        RoomUploadRequest.RoomData roomData = request.getRoom();
        String name = defaultIfBlank(request.getName(), "Uploaded Room");
        String unit = defaultIfBlank(roomData.getUnit(), "meter");
        List<Opening> openings = nullToEmpty(request.getOpenings()).stream()
                .map(this::toOpening)
                .toList();
        List<Furniture> furniture = nullToEmpty(request.getFurniture()).stream()
                .map(this::toFurniture)
                .toList();

        Room room = new Room(null, name, roomData.getWidth(), roomData.getDepth(), roomData.getHeight(),
                unit, openings, furniture, RoomSource.ROOMPLAN, LocalDateTime.now());
        validateFurnitureWithinRoom(room);

        return RoomResponse.from(roomRepository.save(room));
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
        double rotation = snapRotation(furniture.getRotation() == null ? 0 : furniture.getRotation());
        String label = defaultIfBlank(furniture.getLabel(), furniture.getType());

        return new Furniture(furniture.getId(), furniture.getType(), label, furniture.getWidth(),
                furniture.getDepth(), furniture.getHeight(),
                new Position(furniture.getPosition().getX(), furniture.getPosition().getZ()),
                rotation, status);
    }

    private void validateFurnitureWithinRoom(Room room) {
        for (Furniture furniture : room.getFurniture()) {
            double halfWidth = furniture.getWidth() / 2.0;
            double halfDepth = furniture.getDepth() / 2.0;
            double x = furniture.getPosition().getX();
            double z = furniture.getPosition().getZ();

            if (x - halfWidth < 0 || x + halfWidth > room.getWidth()
                    || z - halfDepth < 0 || z + halfDepth > room.getDepth()) {
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

    private double snapRotation(double rotation) {
        double normalized = ((rotation % 360) + 360) % 360;
        double nearest = 0;
        double shortestDistance = circularDistance(normalized, nearest);

        for (double allowed : List.of(90.0, 180.0, 270.0)) {
            double distance = circularDistance(normalized, allowed);
            if (distance < shortestDistance) {
                nearest = allowed;
                shortestDistance = distance;
            }
        }

        return nearest;
    }

    private double circularDistance(double first, double second) {
        double distance = Math.abs(first - second);
        return Math.min(distance, 360 - distance);
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
