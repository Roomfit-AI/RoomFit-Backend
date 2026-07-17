package com.roomfit.room;

import com.roomfit.auth.CurrentGuestContext;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Room/AgentContext/Layout 관련 API가 전부 이 서비스를 거쳐 Room을
 * 조회한다 — 게스트 소유권 검증과 샘플룸 개인화(fork)를 한 곳에서
 * 일관되게 처리하기 위함.
 *
 * ROOMPLAN(실제 업로드한 방)은 소유자가 정확히 일치해야만 접근 가능하고,
 * 아니면 404(ROOM_NOT_FOUND)를 던진다 — 403이 아니라 404인 이유는 "다른
 * 사용자의 방이 존재하긴 한다"는 사실 자체를 노출하지 않기 위해서다.
 *
 * SAMPLE(시드 템플릿, ownerId=null)은 어느 게스트나 접근할 수 있지만,
 * 실제로 그 템플릿에 쓰기 작업(가구 편집, AI 추천 등)이 걸리는 순간 그
 * 게스트 소유의 개인 복사본(fork)을 자동 생성해 그 이후 요청은 전부 그
 * 복사본으로 투명하게 리다이렉트한다. 같은 게스트가 같은 템플릿 id를
 * 반복 참조해도(프론트는 fork가 생긴 것을 알지 못하고 계속 원본 템플릿
 * id로 요청한다) sourceTemplateId로 기존 fork를 찾아 재사용하므로 편집
 * 내용이 자연스럽게 이어진다.
 */
@Service
public class RoomAccessService {

    private final RoomRepository roomRepository;
    private final CurrentGuestContext currentGuestContext;

    public RoomAccessService(RoomRepository roomRepository, CurrentGuestContext currentGuestContext) {
        this.roomRepository = roomRepository;
        this.currentGuestContext = currentGuestContext;
    }

    public Room resolveAccessibleRoom(Long requestedRoomId) {
        Room room = roomRepository.findById(requestedRoomId)
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        String guestId = currentGuestContext.getGuestId();

        if (room.getSource() != RoomSource.SAMPLE) {
            return requireOwnedByCurrentGuest(room, guestId);
        }

        if (room.getOwnerId() == null) {
            // 공유 템플릿 — 이 게스트의 기존 fork가 있으면 그걸 쓰고, 없으면 지금 만든다.
            return roomRepository.findByOwnerIdAndSourceTemplateId(guestId, room.getId())
                    .orElseGet(() -> forkRoom(room, guestId));
        }

        return requireOwnedByCurrentGuest(room, guestId);
    }

    private Room requireOwnedByCurrentGuest(Room room, String guestId) {
        if (!guestId.equals(room.getOwnerId())) {
            throw new CustomException(ErrorCode.ROOM_NOT_FOUND);
        }
        return room;
    }

    private Room forkRoom(Room template, String guestId) {
        List<Wall> walls = template.getWalls().stream().map(this::copyWall).toList();
        List<Opening> openings = template.getOpenings().stream().map(this::copyOpening).toList();
        List<Furniture> furniture = template.getFurniture().stream().map(this::copyFurniture).toList();

        Room fork = new Room(null, template.getName(), template.getWidth(), template.getDepth(),
                template.getHeight(), template.getUnit(), walls, openings, furniture, template.getSource(),
                LocalDateTime.now(), template.getThumbnailBase64(), guestId, template.getId());
        return roomRepository.save(fork);
    }

    private Wall copyWall(Wall wall) {
        return new Wall(wall.getId(),
                new Position(wall.getStart().getX(), wall.getStart().getZ()),
                new Position(wall.getEnd().getX(), wall.getEnd().getZ()),
                wall.getHeight(), wall.getThickness());
    }

    private Opening copyOpening(Opening opening) {
        return new Opening(opening.getId(), opening.getType(), opening.getWall(), opening.getOffset(),
                opening.getWidth(), opening.getHeight(), opening.getSillHeight());
    }

    private Furniture copyFurniture(Furniture item) {
        return new Furniture(item.getId(), item.getType(), item.getLabel(),
                item.getWidth(), item.getDepth(), item.getHeight(),
                new Position(item.getPosition().getX(), item.getPosition().getZ()),
                item.getRotation(), item.getStatus(), item.getProductId(), item.getStyleTags(),
                item.getVariantId());
    }
}
