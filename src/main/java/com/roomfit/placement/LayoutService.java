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
import java.util.Set;
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

        List<Furniture> mergedFurniture = applyPositionOverrides(layout.getFurniture(), request.getFurniture(), room);
        return validationService.validate(room, mergedFurniture);
    }

    public LayoutResponse updateLayout(Long layoutId, LayoutUpdateRequest request) {
        Layout layout = findLayoutOrThrow(layoutId);
        if (layout.isConfirmed()) {
            throw new CustomException(ErrorCode.ALREADY_CONFIRMED);
        }
        Room room = roomRepository.findById(layout.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        List<Furniture> updated = applyPositionOverrides(layout.getFurniture(), request.getFurniture(), room);
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
     */
    private List<Furniture> applyPositionOverrides(List<Furniture> base, List<FurniturePositionDto> overrides, Room room) {
        validateFurnitureArray(base, overrides);

        Map<String, FurniturePositionDto> overrideById = overrides.stream()
                .collect(Collectors.toMap(FurniturePositionDto::getId, o -> o));

        return base.stream().map(f -> {
            FurniturePositionDto override = overrideById.get(f.getId());
            validateRotation(override.getRotation());
            validatePosition(room, f, override);
            return copyWithOverride(f, override);
        }).collect(Collectors.toList());
    }

    private void validateFurnitureArray(List<Furniture> base, List<FurniturePositionDto> overrides) {
        Set<String> baseIds = base.stream()
                .map(Furniture::getId)
                .collect(Collectors.toSet());
        Set<String> requestIds = overrides.stream()
                .map(FurniturePositionDto::getId)
                .collect(Collectors.toSet());

        boolean hasUnknownId = requestIds.stream().anyMatch(id -> !baseIds.contains(id));
        if (hasUnknownId) {
            throw new CustomException(ErrorCode.FURNITURE_NOT_FOUND);
        }
        if (!requestIds.containsAll(baseIds) || requestIds.size() != baseIds.size()) {
            throw new CustomException(ErrorCode.FURNITURE_ARRAY_MISMATCH);
        }
    }

    private void validateRotation(double rotation) {
        if (rotation < 0 || rotation >= 360) {
            throw new CustomException(ErrorCode.INVALID_ROTATION);
        }
    }

    private void validatePosition(Room room, Furniture furniture, FurniturePositionDto override) {
        if (override.getPosition() == null) {
            throw new CustomException(ErrorCode.INVALID_FURNITURE_POSITION);
        }

        double halfWidth = furniture.getWidth() / 2.0;
        double halfDepth = furniture.getDepth() / 2.0;
        double x = override.getPosition().getX();
        double z = override.getPosition().getZ();

        if (x - halfWidth < 0 || x + halfWidth > room.getWidth()
                || z - halfDepth < 0 || z + halfDepth > room.getDepth()) {
            throw new CustomException(ErrorCode.INVALID_FURNITURE_POSITION);
        }
    }

    private Furniture copyWithOverride(Furniture furniture, FurniturePositionDto override) {
        return new Furniture(
                furniture.getId(),
                furniture.getType(),
                furniture.getLabel(),
                furniture.getWidth(),
                furniture.getDepth(),
                furniture.getHeight(),
                new Position(override.getPosition().getX(), override.getPosition().getZ()),
                override.getRotation(),
                furniture.getStatus(),
                furniture.getProductId(),
                furniture.getStyleTags()
        );
    }
}
