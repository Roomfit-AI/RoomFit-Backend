package com.roomfit.room;

import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.room.dto.FurnitureUpdateRequest;
import com.roomfit.room.dto.RoomResponse;
import org.springframework.stereotype.Service;

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
        return roomRepository.findAll().stream()
                .map(RoomResponse::from)
                .toList();
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

    private FurnitureStatus parseStatus(String rawStatus) {
        try {
            return FurnitureStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new CustomException(ErrorCode.INVALID_FURNITURE_STATUS);
        }
    }

    private void validateFurnitureIds(Room room, FurnitureUpdateRequest request) {
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
}
