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
import java.util.LinkedHashMap;
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
    private final FeedbackPlanInterpreter feedbackPlanInterpreter;
    private final DeterministicFeedbackExecutor feedbackExecutor;
    private final ScoreService scoreService;

    public LayoutService(LayoutRepository layoutRepository,
                          AgentContextRepository agentContextRepository,
                          RoomRepository roomRepository,
                          PlacementService placementService,
                          ValidationService validationService,
                          FeedbackPlanInterpreter feedbackPlanInterpreter,
                          DeterministicFeedbackExecutor feedbackExecutor,
                          ScoreService scoreService) {
        this.layoutRepository = layoutRepository;
        this.agentContextRepository = agentContextRepository;
        this.roomRepository = roomRepository;
        this.placementService = placementService;
        this.validationService = validationService;
        this.feedbackPlanInterpreter = feedbackPlanInterpreter;
        this.feedbackExecutor = feedbackExecutor;
        this.scoreService = scoreService;
    }

    public LayoutResponse recommend(RecommendRequest request) {
        AgentContext context = agentContextRepository.findById(request.getContextId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEXT_NOT_FOUND));
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        if (!room.getId().equals(context.getRoomId())) {
            throw new CustomException(ErrorCode.ROOM_CONTEXT_MISMATCH);
        }

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
        ScoreSummary scoreSummary = scoreService.calculate(context, layout.getFurniture(), validationResult);
        PlacementResult scoredPlacementResult = new PlacementResult(placementResult.getStatus(),
                placementResult.getRecommendedFurniture(), scoreSummary);

        return LayoutResponse.ofRecommendation(layout, scoredPlacementResult, validationResult);
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
        AgentContext context = agentContextRepository.findById(layout.getContextId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEXT_NOT_FOUND));

        List<Furniture> updated = applyPositionOverrides(layout.getFurniture(), request.getFurniture(), room);
        layout.setFurniture(updated);
        layoutRepository.save(layout);

        ValidationResult validationResult = validationService.validate(room, updated);
        ScoreSummary scoreSummary = scoreService.calculate(context, updated, validationResult);
        return LayoutResponse.ofUpdate(layout, RecommendationStatus.SUCCESS, scoreSummary, validationResult);
    }

    public ConfirmResponse confirmLayout(Long layoutId) {
        Layout layout = findLayoutOrThrow(layoutId);
        if (layout.isConfirmed()) {
            throw new CustomException(ErrorCode.ALREADY_CONFIRMED);
        }
        layout.confirm();
        layoutRepository.save(layout);

        // 확정된 배치를 Room에도 되반영한다 — 이게 없으면 GET /api/rooms/{roomId}
        // (및 목록 재조회)가 여전히 확정 이전 가구 배치를 보여준다. Layout은
        // Room과 독립된 값 복사 스냅샷이라(Layout.java 참고) 여기서 명시적으로
        // 동기화해야 한다.
        Room room = roomRepository.findById(layout.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));
        room.setFurniture(layout.getFurniture());
        roomRepository.save(room);

        return ConfirmResponse.from(layout);
    }

    public FeedbackResponse feedback(FeedbackRequest request) {
        Layout baseLayout = findLayoutOrThrow(request.getLayoutId());
        AgentContext context = agentContextRepository.findById(baseLayout.getContextId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEXT_NOT_FOUND));
        Room room = roomRepository.findById(baseLayout.getRoomId())
                .orElseThrow(() -> new CustomException(ErrorCode.ROOM_NOT_FOUND));

        FeedbackPlan plan = feedbackPlanInterpreter.interpret(request.getFeedback(), room, baseLayout.getFurniture(), context);
        FeedbackExecution execution = feedbackExecutor.execute(plan, room, baseLayout.getFurniture());
        Layout responseLayout = baseLayout;
        if (execution.result().applied()) {
            responseLayout = new Layout(baseLayout.getRoomId(), baseLayout.getContextId(), execution.furniture());
            layoutRepository.save(responseLayout);
        }

        ValidationResult validationResult = validationService.validate(room, execution.furniture());
        ScoreSummary scoreSummary = scoreService.calculate(context, execution.furniture(), validationResult);
        return FeedbackResponse.of(responseLayout, RecommendationStatus.SUCCESS,
                scoreSummary, validationResult, interpretedPlan(plan), execution.result());
    }

    private Map<String, Object> interpretedPlan(FeedbackPlan plan) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", plan.source().name());
        result.put("fallbackUsed", plan.fallbackUsed());
        result.put("version", plan.version());
        result.put("requestKind", plan.requestKind().name());
        result.put("targetFurnitureId", plan.furnitureId());
        result.put("targetFurniture", plan.furnitureType());
        result.put("operations", plan.operations().stream().map(operation -> operation.type().name()).toList());
        result.put("operationIds", plan.operations().stream().map(FeedbackOperation::operationId).toList());
        result.put("reason", plan.reason());
        if (plan.clarification() != null) {
            result.put("clarificationQuestion", plan.clarification().question());
        }
        if (plan.source() == FeedbackSource.RULE_BASED && !plan.operations().isEmpty()) {
            FeedbackOperation operation = plan.operations().get(0);
            if (operation.type() == FeedbackOperationType.REPLACE_PRODUCT && operation.constraints().largerThanCurrent()) {
                result.put("rawIntent", "LARGER_DESK");
                result.put("deskMinWidth", 1.4);
            }
            if (operation.type() == FeedbackOperationType.REPLACE_PRODUCT && operation.constraints().storagePreferred()) {
                result.put("storagePriority", "HIGH");
            }
            if (operation.type() == FeedbackOperationType.MOVE && "방이 넓어 보이게".equals(plan.reason())) {
                result.put("openSpacePriority", "HIGH");
            }
        }
        return result;
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
        if (overrides == null || overrides.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        boolean hasInvalidItem = overrides.stream()
                .anyMatch(override -> override == null || isBlank(override.getId()));
        if (hasInvalidItem) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }

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
                furniture.getStyleTags(),
                furniture.getVariantId()
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<Furniture> applyFeedbackIntent(Room room, List<Furniture> furniture, FeedbackIntent intent) {
        return switch (intent.type()) {
            case LARGER_DESK -> furniture.stream()
                    .map(item -> copyWithLargerDesk(room, item))
                    .collect(Collectors.toList());
            case STORAGE_PRIORITY -> applyStoragePriority(furniture);
            case OPEN_SPACE_PRIORITY -> applyOpenSpacePriority(furniture);
        };
    }

    private Furniture copyWithLargerDesk(Room room, Furniture furniture) {
        if (!"desk".equals(furniture.getType())) {
            return copyFurniture(furniture, furniture.getPosition(), furniture.getRotation(),
                    furniture.getWidth(), furniture.getDepth(), furniture.getHeight(), furniture.getStatus());
        }

        double width = Math.max(furniture.getWidth(), 1.4);
        Position position = clampPositionInsideRoom(room, furniture.getPosition(), width,
                furniture.getDepth(), furniture.getRotation());

        return copyFurniture(furniture, position, furniture.getRotation(), width,
                furniture.getDepth(), furniture.getHeight(), furniture.getStatus());
    }

    private Position clampPositionInsideRoom(Room room, Position position, double width,
                                             double depth, double rotation) {
        FurnitureFootprint footprint = FurnitureFootprint.from(width, depth, rotation);
        double minX = footprint.effectiveWidth() / 2.0;
        double maxX = room.getWidth() - footprint.effectiveWidth() / 2.0;
        double minZ = footprint.effectiveDepth() / 2.0;
        double maxZ = room.getDepth() - footprint.effectiveDepth() / 2.0;

        return new Position(
                clamp(position.getX(), minX, maxX),
                clamp(position.getZ(), minZ, maxZ)
        );
    }

    private double clamp(double value, double min, double max) {
        if (max < min) {
            return (min + max) / 2.0;
        }
        return Math.max(min, Math.min(max, value));
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
                furniture.getStyleTags(),
                furniture.getVariantId()
        );
    }

}
