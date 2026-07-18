package com.roomfit.room;

import com.roomfit.client.ClientScope;
import com.roomfit.client.ClientScopeContext;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class RoomAccessService {

    private final RoomRepository roomRepository;
    private final ClientScopeContext clientScopeContext;

    public RoomAccessService(RoomRepository roomRepository, ClientScopeContext clientScopeContext) {
        this.roomRepository = roomRepository;
        this.clientScopeContext = clientScopeContext;
    }

    public ClientScope currentScope() {
        return clientScopeContext.current();
    }

    public Room findReadableRoom(Long roomId) {
        Room room = findExistingRoom(roomId);
        if (canRead(room, currentScope())) {
            return room;
        }
        throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
    }

    public Room findWritableRoom(Long roomId) {
        Room room = findExistingRoom(roomId);
        if (canWrite(room, currentScope())) {
            return room;
        }
        // Do not disclose whether another client's room exists.
        throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
    }

    private Room findExistingRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
    }

    private boolean canRead(Room room, ClientScope scope) {
        if (!scope.enabled() || room.getSource() == RoomSource.SAMPLE) {
            return true;
        }
        return owns(room, scope);
    }

    private boolean canWrite(Room room, ClientScope scope) {
        if (!scope.enabled()) {
            return true;
        }
        // Header-less legacy clients retain the established sample demo flow
        // until web migration is complete. Scoped clients must copy samples.
        if (room.getSource() == RoomSource.SAMPLE) {
            return scope.legacy();
        }
        return owns(room, scope);
    }

    private boolean owns(Room room, ClientScope scope) {
        String owner = room.getClientScope();
        if (owner == null || owner.isBlank()) {
            return scope.legacy();
        }
        return owner.equals(scope.id());
    }
}
