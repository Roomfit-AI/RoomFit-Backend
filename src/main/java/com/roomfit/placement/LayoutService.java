package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.repository.AgentContextRepository;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.placement.dto.*;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
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
    private final FeedbackParserService feedbackParserService;

    public LayoutService(LayoutRepository layoutRepository,
                          AgentContextRepository agentContextRepository,
                          RoomRepository roomRepository,
                          PlacementService placementService,
                          ValidationService validationService,
                          FeedbackParserService feedbackParserService) {
        this.layoutRepository = layoutRepository;
        this.agentContextRepository = agentContextRepository;
        this.roomRepository = roomRepository;
        this.placementService = placementService;
        this.validationService = validationService;
        this.feedbackParserService = feedbackParserService;
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

    public FeedbackResponse feedback(FeedbackRequest request) {
        Layout baseLayout = findLayoutOrThrow(request.getLayoutId());
        Room room = roomRepository.findById(baseLayout.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        FeedbackParserService.FeedbackIntent intent = feedbackParserService.parse(request.getFeedback());
        List<Furniture> recommended = applyFeedbackIntent(baseLayout.getFurniture(), intent);

        Layout newLayout = new Layout(baseLayout.getRoomId(), baseLayout.getContextId(), recommended);
        layoutRepository.save(newLayout);

        ValidationResult validationResult = validationService.validate(room, recommended);
        return FeedbackResponse.of(newLayout, RecommendationStatus.SUCCESS,
                ScoreSummary.defaultSummary(), validationResult, intent.interpretedIntent());
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
        FurnitureStatus status = parseFurnitureStatus(override.getStatus(), furniture.getStatus());
        return new Furniture(
                furniture.getId(),
                furniture.getType(),
                furniture.getLabel(),
                furniture.getWidth(),
                furniture.getDepth(),
                furniture.getHeight(),
                new Position(override.getPosition().getX(), override.getPosition().getZ()),
                override.getRotation(),
                status,
                furniture.getProductId(),
                furniture.getStyleTags()
        );
    }

    private FurnitureStatus parseFurnitureStatus(String rawStatus, FurnitureStatus fallback) {
        if (rawStatus == null) {
            return fallback;
        }
        try {
            return FurnitureStatus.valueOf(rawStatus);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_FURNITURE_STATUS);
        }
    }

    private List<Furniture> applyFeedbackIntent(List<Furniture> furniture, FeedbackParserService.FeedbackIntent intent) {
        return switch (intent.type()) {
            case LARGER_DESK -> furniture.stream()
                    .map(this::copyWithLargerDesk)
                    .collect(Collectors.toList());
            case STORAGE_PRIORITY -> applyStoragePriority(furniture);
            case OPEN_SPACE_PRIORITY -> applyOpenSpacePriority(furniture);
        };
    }

    private Furniture copyWithLargerDesk(Furniture furniture) {
        double width = "desk".equals(furniture.getType()) ? Math.max(furniture.getWidth(), 1.4) : furniture.getWidth();
        return copyFurniture(furniture, furniture.getPosition(), furniture.getRotation(), width,
                furniture.getDepth(), furniture.getHeight(), furniture.getStatus());
    }

    private List<Furniture> applyStoragePriority(List<Furniture> furniture) {
        boolean hasStorage = furniture.stream().anyMatch(item -> "storage".equals(item.getType()));
        List<Furniture> updated = furniture.stream()
                .map(item -> {
                    if (!"storage".equals(item.getType())) {
                        return copyFurniture(item, item.getPosition(), item.getRotation(),
                                item.getWidth(), item.getDepth(), item.getHeight(), item.getStatus());
                    }
                    return copyFurniture(item, item.getPosition(), item.getRotation(),
                            Math.max(item.getWidth(), 1.0), Math.max(item.getDepth(), 0.45),
                            Math.max(item.getHeight(), 1.8), item.getStatus());
                })
                .collect(Collectors.toList());

        if (!hasStorage) {
            updated.add(new Furniture("storage-feedback-1", "storage", "storage",
                    1.0, 0.45, 1.8, new Position(0.7, 3.6), 0,
                    FurnitureStatus.RECOMMENDED, null, List.of()));
        }
        return updated;
    }

    private List<Furniture> applyOpenSpacePriority(List<Furniture> furniture) {
        return furniture.stream()
                .map(item -> {
                    Position position = switch (item.getType()) {
                        case "bed" -> new Position(0.8, 1.4);
                        case "desk" -> new Position(2.4, 1.0);
                        case "chair" -> new Position(2.4, 1.7);
                        case "storage" -> new Position(2.6, 3.7);
                        default -> item.getPosition();
                    };
                    return copyFurniture(item, position, item.getRotation(),
                            item.getWidth(), item.getDepth(), item.getHeight(), item.getStatus());
                })
                .collect(Collectors.toList());
    }

    private Furniture copyFurniture(Furniture furniture, Position position, double rotation,
                                     double width, double depth, double height, FurnitureStatus status) {
        return new Furniture(
                furniture.getId(),
                furniture.getType(),
                furniture.getLabel(),
                width,
                depth,
                height,
                new Position(position.getX(), position.getZ()),
                rotation,
                status,
                furniture.getProductId(),
                furniture.getStyleTags()
        );
    }

}
