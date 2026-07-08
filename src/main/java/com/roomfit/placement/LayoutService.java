package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.repository.AgentContextRepository;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.placement.dto.*;
import com.roomfit.room.Furniture;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import com.roomfit.room.RoomRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LayoutService {

    private final LayoutRepository layoutRepository;
    private final AgentContextRepository agentContextRepository;
    private final RoomRepository roomRepository;
    private final PlacementService placementService; // 규칙기반/AI기반 구현체를 DI로 교체 가능
    private final ValidationService validationService;

    public LayoutService(LayoutRepository layoutRepository,
                          AgentContextRepository agentContextRepository,
                          RoomRepository roomRepository,
                          PlacementService placementService,
                          ValidationService validationService) {
        this.layoutRepository = layoutRepository;
        this.agentContextRepository = agentContextRepository;
        this.roomRepository = roomRepository;
        this.placementService = placementService;
        this.validationService = validationService;
    }

    public LayoutResponse recommend(RecommendRequest request) {
        AgentContext context = agentContextRepository.findById(request.getContextId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEXT_NOT_FOUND));
        Room room = roomRepository.findById(context.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        PlacementResult placementResult;
        try {
            placementResult = placementService.recommend(context, room);
        } catch (Exception e) {
            // TODO: AI Agent 호출 실패 시 규칙 기반 fallback 로직으로 재시도.
            // 지금은 스켈레톤이라 바로 예외 처리.
            throw new CustomException(ErrorCode.RECOMMENDATION_FAILED);
        }

        Layout layout = new Layout(room.getId(), context.getId(), placementResult.getRecommendedFurniture());
        layoutRepository.save(layout);

        ValidationResult validationResult = validationService.validate(room, layout.getFurniture());

        return LayoutResponse.ofRecommendation(layout, placementResult, validationResult);
    }

    public ValidationResult validateOnly(ValidateRequest request) {
        Layout layout = findLayoutOrThrow(request.getLayoutId());
        Room room = roomRepository.findById(layout.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        List<Furniture> mergedFurniture = applyPositionOverrides(layout.getFurniture(), request.getFurniture());
        return validationService.validate(room, mergedFurniture);
    }

    public LayoutResponse updateLayout(Long layoutId, LayoutUpdateRequest request) {
        Layout layout = findLayoutOrThrow(layoutId);
        if (layout.isConfirmed()) {
            throw new CustomException(ErrorCode.ALREADY_CONFIRMED);
        }
        Room room = roomRepository.findById(layout.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        List<Furniture> updated = applyPositionOverrides(layout.getFurniture(), request.getFurniture());
        layout.setFurniture(updated);
        layoutRepository.save(layout);

        ValidationResult validationResult = validationService.validate(room, updated);
        return LayoutResponse.ofUpdate(layout, validationResult);
    }

    public ConfirmResponse confirmLayout(Long layoutId) {
        Layout layout = findLayoutOrThrow(layoutId);
        if (layout.isConfirmed()) {
            throw new CustomException(ErrorCode.ALREADY_CONFIRMED);
        }
        layout.confirm();
        layoutRepository.save(layout);
        return ConfirmResponse.from(layout);
    }

    private Layout findLayoutOrThrow(Long layoutId) {
        return layoutRepository.findById(layoutId)
                .orElseThrow(() -> new CustomException(ErrorCode.LAYOUT_NOT_FOUND));
    }

    /**
     * 기존 furniture 리스트에 position/rotation override를 적용해 새 리스트를 만든다.
     * TODO: 방 범위를 벗어나는 좌표는 INVALID_FURNITURE_POSITION 예외 처리 필요.
     */
    private List<Furniture> applyPositionOverrides(List<Furniture> base, List<FurniturePositionDto> overrides) {
        Map<String, FurniturePositionDto> overrideById = overrides.stream()
                .collect(Collectors.toMap(FurniturePositionDto::getId, o -> o));

        return base.stream().map(f -> {
            FurniturePositionDto override = overrideById.get(f.getId());
            if (override != null) {
                f.setPosition(new Position(override.getPosition().getX(), override.getPosition().getZ()));
                f.setRotation(override.getRotation());
            }
            return f;
        }).collect(Collectors.toList());
    }
}
