package com.roomfit.placement;

import com.roomfit.agent.domain.AgentContext;
import com.roomfit.agent.repository.AgentContextRepository;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.placement.dto.*;
import com.roomfit.product.catalog.GeneratedFurnitureCatalog;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureBoundary;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Position;
import com.roomfit.room.Room;
import com.roomfit.room.RoomAccessService;
import com.roomfit.room.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LayoutService {

    private final LayoutRepository layoutRepository;
    private final AgentContextRepository agentContextRepository;
    private final RoomRepository roomRepository;
    private final RoomAccessService roomAccessService;
    private final PlacementService placementService; // 규칙기반/AI기반 구현체를 DI로 교체 가능
    private final ValidationService validationService;
    private final FeedbackPlanInterpreter feedbackPlanInterpreter;
    private final DeterministicFeedbackExecutor feedbackExecutor;
    private final ScoreService scoreService;
    private final FurnitureAdditionPolicy furnitureAdditionPolicy;
    private final FurnitureDomainPolicy furnitureDomainPolicy;

    public LayoutService(LayoutRepository layoutRepository,
                          AgentContextRepository agentContextRepository,
                          RoomRepository roomRepository,
                          RoomAccessService roomAccessService,
                          PlacementService placementService,
                          ValidationService validationService,
                          FeedbackPlanInterpreter feedbackPlanInterpreter,
                          DeterministicFeedbackExecutor feedbackExecutor,
                          ScoreService scoreService,
                          FurnitureAdditionPolicy furnitureAdditionPolicy,
                          FurnitureDomainPolicy furnitureDomainPolicy) {
        this.layoutRepository = layoutRepository;
        this.agentContextRepository = agentContextRepository;
        this.roomRepository = roomRepository;
        this.roomAccessService = roomAccessService;
        this.placementService = placementService;
        this.validationService = validationService;
        this.feedbackPlanInterpreter = feedbackPlanInterpreter;
        this.feedbackExecutor = feedbackExecutor;
        this.scoreService = scoreService;
        this.furnitureAdditionPolicy = furnitureAdditionPolicy;
        this.furnitureDomainPolicy = furnitureDomainPolicy;
    }

    public LayoutResponse recommend(RecommendRequest request) {
        AgentContext context = agentContextRepository.findById(request.getContextId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEXT_NOT_FOUND));
        Room room = roomAccessService.findWritableRoom(request.getRoomId());

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

        furnitureDomainPolicy.validateFinalState(placementResult.getRecommendedFurniture());
        ValidationResult validationResult = validationService.validate(room, placementResult.getRecommendedFurniture());
        // A PlacementService may use a provisional summary while it constructs a
        // candidate. The API must always expose the score calculated from this
        // exact validation result, including normal FAILED outcomes.
        ScoreSummary scoreSummary = scoreService.calculate(context, placementResult.getRecommendedFurniture(), validationResult);
        PlacementResult scoredPlacementResult = new PlacementResult(placementResult.getStatus(),
                placementResult.getRecommendedFurniture(), scoreSummary,
                placementResult.getRequestedFurnitureCount(), placementResult.getPlacedFurnitureCount(),
                placementResult.getUnplacedFurniture(), placementResult.getRecommendationStatus(),
                placementResult.getWarningCode(), placementResult.getMessage());
        if (placementResult.getRecommendationStatus() == RecommendationExecutionStatus.FAILED) {
            // A normal lack of physical space is a valid recommendation outcome, not a server error.
            // Do not persist an empty or invalid recommendation snapshot.
            return LayoutResponse.ofRecommendationFailure(room.getId(), scoredPlacementResult, validationResult);
        }
        // Legacy scripted sample layouts intentionally preserve their historical
        // composition (including decorative rug/table overlap) and do not carry
        // request-instance metadata. New deterministic recommendations must be
        // hard-valid before they are persisted.
        if (placementResult.getRequestedFurnitureCount() > 0 && !isHardValid(validationResult)) {
            throw new CustomException(ErrorCode.RECOMMENDATION_FAILED);
        }
        Layout layout = new Layout(room.getId(), context.getId(), placementResult.getRecommendedFurniture());
        layoutRepository.save(layout);

        return LayoutResponse.ofRecommendation(layout, scoredPlacementResult, validationResult);
    }

    private boolean isHardValid(ValidationResult result) {
        return result.isCollisionFree() && result.isBoundaryValid() && result.isDoorClearance()
                && result.isWindowClearance() && result.isPathSecured();
    }

    public LayoutResponse getLayout(Long layoutId) {
        Layout layout = findLayoutOrThrow(layoutId);
        roomAccessService.findReadableRoom(layout.getRoomId());
        return snapshotResponse(layout);
    }

    public LayoutResponse getLatestConfirmedLayout(Long roomId) {
        roomAccessService.findReadableRoom(roomId);
        Layout layout = layoutRepository.findFirstByRoomIdAndConfirmedTrueOrderByConfirmedAtDesc(roomId)
                .orElseThrow(() -> new CustomException(ErrorCode.LAYOUT_NOT_FOUND));
        return snapshotResponse(layout);
    }

    public LayoutResponse createDraft(Long sourceLayoutId) {
        Layout source = findLayoutOrThrow(sourceLayoutId);
        roomAccessService.findWritableRoom(source.getRoomId());
        if (!source.isConfirmed()) {
            throw new CustomException(ErrorCode.LAYOUT_NOT_CONFIRMED);
        }

        Room room = roomAccessService.findWritableRoom(source.getRoomId());
        List<Furniture> furniture = deepCopyFurniture(source.getFurniture());
        ValidationResult validationResult = validationService.validate(room, furniture);
        if (!validationResult.isBoundaryValid()) {
            throw new CustomException(ErrorCode.INVALID_FURNITURE_POSITION);
        }

        Layout draft = new Layout(source.getRoomId(), source.getContextId(), furniture, source.getId());
        layoutRepository.save(draft);
        AgentContext context = agentContextRepository.findById(source.getContextId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEXT_NOT_FOUND));
        ScoreSummary scoreSummary = scoreService.calculate(context, furniture, validationResult);
        return LayoutResponse.ofSnapshot(draft, scoreSummary, validationResult);
    }

    public ValidationResult validateOnly(ValidateRequest request) {
        Layout layout = findLayoutOrThrow(request.getLayoutId());
        Room room = roomAccessService.findReadableRoom(layout.getRoomId());

        List<Furniture> mergedFurniture = applyPositionOverrides(layout.getFurniture(), request.getFurniture(), room);
        furnitureDomainPolicy.validateFinalState(mergedFurniture);
        return validationService.validate(room, mergedFurniture);
    }

    @Transactional
    public LayoutResponse updateLayout(Long layoutId, LayoutUpdateRequest request) {
        Layout layout = findLayoutOrThrow(layoutId);
        roomAccessService.findWritableRoom(layout.getRoomId());
        if (layout.isConfirmed()) {
            throw new CustomException(ErrorCode.ALREADY_CONFIRMED);
        }
        Room room = roomAccessService.findWritableRoom(layout.getRoomId());
        AgentContext context = agentContextRepository.findById(layout.getContextId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEXT_NOT_FOUND));

        List<Furniture> updated = applyPositionOverrides(layout.getFurniture(), request.getFurniture(), room);
        furnitureDomainPolicy.validateFinalState(updated);
        ValidationResult validationResult = validationService.validate(room, updated);
        ScoreSummary scoreSummary = scoreService.calculate(context, updated, validationResult);
        layout.setFurniture(updated);
        layoutRepository.save(layout);
        return LayoutResponse.ofUpdate(layout, RecommendationStatus.SUCCESS, scoreSummary, validationResult);
    }

    @Transactional
    public LayoutResponse addFurniture(Long layoutId, DraftFurnitureAdditionRequest request) {
        Layout layout = findLayoutOrThrow(layoutId);
        roomAccessService.findWritableRoom(layout.getRoomId());
        if (layout.isConfirmed()) {
            throw new CustomException(ErrorCode.ALREADY_CONFIRMED);
        }
        if (request == null || request.getContextId() == null) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }

        Room room = roomAccessService.findWritableRoom(layout.getRoomId());
        AgentContext context = agentContextRepository.findById(request.getContextId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEXT_NOT_FOUND));
        if (!room.getId().equals(context.getRoomId())) {
            throw new CustomException(ErrorCode.ROOM_CONTEXT_MISMATCH);
        }

        List<String> requestedTypes = context.getRequiredItems().stream()
                .map(GeneratedFurnitureCatalog.get()::normalizeType)
                .filter(type -> type != null && !type.isBlank())
                .toList();

        furnitureAdditionPolicy.validate(layout.getFurniture(), requestedTypes);

        List<FeedbackOperation> operations = new ArrayList<>();
        for (int index = 0; index < requestedTypes.size(); index++) {
            String type = requestedTypes.get(index);
            operations.add(new FeedbackOperation(
                    "add-selection-" + (index + 1),
                    FeedbackOperationType.ADD_FURNITURE,
                    new FeedbackTargetSelector("", type, ""),
                    null,
                    new FeedbackPlacement(FeedbackRelation.NEAR_WALL, null, null, null),
                    null,
                    new FeedbackProductRequirements(type, FeedbackSizePreference.ANY, false, List.of()),
                    null,
                    List.of()
            ));
        }

        List<Furniture> updated = deepCopyFurniture(layout.getFurniture());
        if (!operations.isEmpty()) {
            FeedbackPlan plan = new FeedbackPlan(
                    "2.0",
                    operations.size() == 1 ? FeedbackRequestKind.DIRECT : FeedbackRequestKind.COMPOSITE,
                    operations,
                    List.of(),
                    null,
                    "Add Furniture selection",
                    FeedbackSource.RULE_BASED,
                    false
            );
            FeedbackExecution execution = feedbackExecutor.execute(plan, room, updated, context,
                    FurnitureAdditionPolicy.MAX_NEW_ADDITIONS);
            if (execution.result().operationsApplied().size() != operations.size()) {
                throw new CustomException(ErrorCode.FURNITURE_ADDITION_FAILED);
            }
            updated = deepCopyFurniture(execution.furniture());
        }

        furnitureDomainPolicy.validateFinalState(updated);
        ValidationResult validationResult = validationService.validate(room, updated);
        ScoreSummary scoreSummary = scoreService.calculate(context, updated, validationResult);
        layout.setContextId(context.getId());
        layout.setFurniture(updated);
        layoutRepository.save(layout);
        return LayoutResponse.ofUpdate(layout, RecommendationStatus.SUCCESS, scoreSummary, validationResult);
    }

    @Transactional
    public ConfirmResponse confirmLayout(Long layoutId) {
        Layout layout = findLayoutOrThrow(layoutId);
        roomAccessService.findWritableRoom(layout.getRoomId());
        furnitureDomainPolicy.validateFinalState(layout.getFurniture());
        if (layout.isConfirmed()) {
            throw new CustomException(ErrorCode.ALREADY_CONFIRMED);
        }
        Room room = roomAccessService.findWritableRoom(layout.getRoomId());
        if (!isHardValid(validationService.validate(room, layout.getFurniture()))) {
            throw new CustomException(ErrorCode.INVALID_FURNITURE_POSITION);
        }
        layout.confirm();
        layoutRepository.save(layout);

        // 확정된 배치를 Room에도 되반영한다 — 이게 없으면 GET /api/rooms/{roomId}
        // (및 목록 재조회)가 여전히 확정 이전 가구 배치를 보여준다. Layout은
        // Room과 독립된 값 복사 스냅샷이라(Layout.java 참고) 여기서 명시적으로
        // 동기화해야 한다.
        room.setFurniture(layout.getFurniture());
        roomRepository.save(room);

        return ConfirmResponse.from(layout);
    }

    @Transactional
    public FeedbackResponse feedback(FeedbackRequest request) {
        Layout baseLayout = findLayoutOrThrow(request.getLayoutId());
        roomAccessService.findWritableRoom(baseLayout.getRoomId());
        AgentContext context = agentContextRepository.findById(baseLayout.getContextId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEXT_NOT_FOUND));
        Room room = roomAccessService.findWritableRoom(baseLayout.getRoomId());

        FeedbackPlan plan = feedbackPlanInterpreter.interpret(request.getFeedback(), room, baseLayout.getFurniture(), context,
                request.getSelectedFurnitureId());
        FeedbackExecution execution = feedbackExecutor.execute(plan, room, baseLayout.getFurniture(), context);
        furnitureDomainPolicy.validateFinalState(execution.furniture());
        ValidationResult validationResult = validationService.validate(room, execution.furniture());
        ScoreSummary scoreSummary = scoreService.calculate(context, execution.furniture(), validationResult);
        Layout responseLayout = baseLayout;
        if (execution.result().applied()) {
            responseLayout = new Layout(baseLayout.getRoomId(), baseLayout.getContextId(),
                    deepCopyFurniture(execution.furniture()), baseLayout.getId());
            layoutRepository.save(responseLayout);
        }

        return FeedbackResponse.of(responseLayout, RecommendationStatus.SUCCESS,
                scoreSummary, validationResult, interpretedPlan(plan), execution.result(),
                feedbackStatus(plan, execution), operationResults(plan, execution, baseLayout.getFurniture()),
                clarifications(plan, execution, baseLayout.getFurniture()));
    }

    private FeedbackStatus feedbackStatus(FeedbackPlan plan, FeedbackExecution execution) {
        long applied = execution.operationResults().stream()
                .filter(result -> result.status() == FeedbackOperationExecution.Status.APPLIED)
                .count();
        if (applied == plan.operations().size() && !plan.operations().isEmpty()) {
            return FeedbackStatus.SUCCESS;
        }
        if (applied > 0) {
            return FeedbackStatus.PARTIAL_SUCCESS;
        }
        if (plan.needsClarification() || execution.operationResults().stream()
                .anyMatch(result -> needsClarification(result.reasonCode()))) {
            return FeedbackStatus.NEEDS_CLARIFICATION;
        }
        return FeedbackStatus.FAILED;
    }

    /**
     * The executor exposes compact internal execution data. Convert it here so
     * the HTTP response remains additive and is ordered exactly as the Plan.
     */
    private List<FeedbackOperationResult> operationResults(FeedbackPlan plan, FeedbackExecution execution,
                                                            List<Furniture> originalFurniture) {
        Map<String, FeedbackOperationExecution> executions = execution.operationResults().stream()
                .collect(Collectors.toMap(FeedbackOperationExecution::operationId, result -> result,
                        (first, ignored) -> first, LinkedHashMap::new));
        return plan.operations().stream().map(operation -> {
            FeedbackOperationExecution executionResult = executions.get(operation.operationId());
            if (executionResult == null) {
                return new FeedbackOperationResult(operation.operationId(), operation.type(),
                        FeedbackOperationResult.Status.FAILED, "INVALID_OPERATION",
                        operationMessage(operation.type(), FeedbackOperationResult.Status.FAILED, "INVALID_OPERATION"),
                        targetFurnitureId(operation, null), null, null, null);
            }
            FeedbackOperationResult.Status status = publicOperationStatus(executionResult);
            String affectedId = executionResult.affectedFurnitureId();
            Furniture resultFurniture = findFurniture(execution.furniture(), affectedId).orElse(null);
            if (resultFurniture == null && status == FeedbackOperationResult.Status.APPLIED
                    && operation.type() == FeedbackOperationType.REMOVE_FURNITURE) {
                resultFurniture = findFurniture(originalFurniture, affectedId).orElse(null);
            }
            String targetId = targetFurnitureId(operation, affectedId);
            return new FeedbackOperationResult(operation.operationId(), operation.type(), status,
                    executionResult.reasonCode(),
                    operationMessage(operation.type(), status, executionResult.reasonCode()),
                    targetId, affectedId,
                    resultFurniture == null ? null : resultFurniture.getProductId(),
                    resultFurniture == null ? null : resultFurniture.getVariantId());
        }).toList();
    }

    private List<FeedbackClarificationResponse> clarifications(FeedbackPlan plan, FeedbackExecution execution,
                                                                 List<Furniture> originalFurniture) {
        List<FeedbackClarificationResponse> result = new ArrayList<>();
        if (plan.needsClarification()) {
            FeedbackClarification clarification = plan.clarification();
            String type = clarification == null ? "" : clarification.targetFurnitureType();
            List<FeedbackClarificationResponse.Candidate> candidates = clarificationCandidates(type, "", originalFurniture);
            result.add(new FeedbackClarificationResponse(candidates.size() > 1 ? "AMBIGUOUS_TARGET" : "NEEDS_CLARIFICATION",
                    clarificationQuestion(type, false), null, "targetFurnitureId",
                    candidates));
        }
        Map<String, FeedbackOperation> operations = plan.operations().stream()
                .collect(Collectors.toMap(FeedbackOperation::operationId, operation -> operation,
                        (first, ignored) -> first, LinkedHashMap::new));
        for (FeedbackOperationExecution executionResult : execution.operationResults()) {
            if (!needsClarification(executionResult.reasonCode())) continue;
            FeedbackOperation operation = operations.get(executionResult.operationId());
            if (operation == null) continue;
            boolean reference = executionResult.reasonCode().contains("REFERENCE");
            FeedbackTargetSelector target = reference ? operation.referenceTarget() : operation.target();
            String type = target == null ? "" : target.furnitureType();
            String keyword = target == null ? "" : target.labelKeyword();
            result.add(new FeedbackClarificationResponse(executionResult.reasonCode(),
                    clarificationQuestion(type, reference), operation.operationId(),
                    reference ? "referenceTargetFurnitureId" : "targetFurnitureId",
                    clarificationCandidates(type, keyword, originalFurniture)));
        }
        return List.copyOf(result);
    }

    private FeedbackOperationResult.Status publicOperationStatus(FeedbackOperationExecution result) {
        if (result.status() == FeedbackOperationExecution.Status.APPLIED) {
            return FeedbackOperationResult.Status.APPLIED;
        }
        if (result.status() == FeedbackOperationExecution.Status.SKIPPED) {
            return FeedbackOperationResult.Status.SKIPPED_DEPENDENCY;
        }
        return needsClarification(result.reasonCode())
                ? FeedbackOperationResult.Status.NEEDS_CLARIFICATION
                : FeedbackOperationResult.Status.FAILED;
    }

    private boolean needsClarification(String reasonCode) {
        if (reasonCode == null) return false;
        return Set.of("NEEDS_CLARIFICATION", "AMBIGUOUS_TARGET", "AMBIGUOUS_REFERENCE_TARGET",
                        "UNSUPPORTED_LOCATION_HINT", "UNSUPPORTED_REFERENCE_LOCATION_HINT")
                .contains(reasonCode);
    }

    private String targetFurnitureId(FeedbackOperation operation, String affectedId) {
        if (operation.target() != null && !operation.target().furnitureId().isBlank()) {
            return operation.target().furnitureId();
        }
        return operation.type() == FeedbackOperationType.ADD_FURNITURE ? null : affectedId;
    }

    private Optional<Furniture> findFurniture(List<Furniture> furniture, String furnitureId) {
        if (furnitureId == null) return Optional.empty();
        return furniture.stream().filter(item -> furnitureId.equals(item.getId())).findFirst();
    }

    private List<FeedbackClarificationResponse.Candidate> clarificationCandidates(String type, String labelKeyword,
                                                                                    List<Furniture> furniture) {
        if (type == null || type.isBlank()) return List.of();
        String normalizedKeyword = labelKeyword == null ? "" : labelKeyword.toLowerCase(java.util.Locale.ROOT);
        return furniture.stream()
                .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .filter(item -> GeneratedFurnitureCatalog.get().sameType(type, item.getType()))
                .filter(item -> normalizedKeyword.isBlank() || (item.getLabel() != null
                        && item.getLabel().toLowerCase(java.util.Locale.ROOT).contains(normalizedKeyword)))
                .sorted(Comparator.comparingDouble((Furniture item) -> item.getPosition().getX())
                        .thenComparingDouble(item -> item.getPosition().getZ())
                        .thenComparing(Furniture::getId))
                .limit(10)
                .map(item -> new FeedbackClarificationResponse.Candidate(item.getId(), item.getType(), item.getLabel()))
                .toList();
    }

    private String clarificationQuestion(String type, boolean reference) {
        String subject = type == null || type.isBlank() ? "가구" : type + " 가구";
        return reference ? subject + " 중 기준으로 사용할 가구를 선택해주세요."
                : subject + " 중 변경할 가구를 선택해주세요.";
    }

    private String operationMessage(FeedbackOperationType type, FeedbackOperationResult.Status status,
                                    String reasonCode) {
        if (status == FeedbackOperationResult.Status.APPLIED) {
            return switch (type) {
                case MOVE -> "가구 위치를 이동했습니다.";
                case ROTATE -> "가구 방향을 회전했습니다.";
                case REPLACE_PRODUCT -> "가구 제품을 교체했습니다.";
                case ADD_FURNITURE -> "가구를 추가했습니다.";
                case REMOVE_FURNITURE -> "가구를 제거했습니다.";
                case SWAP_FURNITURE -> "가구 종류를 교체했습니다.";
                default -> "작업을 적용했습니다.";
            };
        }
        return switch (reasonCode == null ? "" : reasonCode) {
            case "DEPENDENCY_NOT_APPLIED" -> "선행 작업이 적용되지 않아 실행하지 않았습니다.";
            case "AMBIGUOUS_TARGET" -> "변경할 가구를 하나로 특정할 수 없습니다.";
            case "AMBIGUOUS_REFERENCE_TARGET" -> "기준 가구를 하나로 특정할 수 없습니다.";
            case "TARGET_NOT_FOUND" -> "요청한 가구를 찾을 수 없습니다.";
            case "REFERENCE_TARGET_NOT_FOUND" -> "기준 가구를 찾을 수 없습니다.";
            case "NO_RENDERABLE_PRODUCT" -> "렌더링 가능한 제품을 찾을 수 없습니다.";
            case "NO_VALID_ADD_PLACEMENT" -> "추가 가구를 놓을 유효한 위치를 찾을 수 없습니다.";
            case "NO_VALID_SWAP_PLACEMENT" -> "교체 가구를 놓을 유효한 위치를 찾을 수 없습니다.";
            case "NO_VALID_BOUNDARY_PLACEMENT" -> "가구를 방 경계 안에 배치할 수 없습니다.";
            case "ROTATION_OUT_OF_BOUNDS" -> "회전하면 가구가 방 경계를 벗어납니다.";
            case "UNSUPPORTED_LOCATION_HINT", "UNSUPPORTED_REFERENCE_LOCATION_HINT" ->
                    "현재 방 정보로 위치 표현을 안전하게 판별할 수 없습니다.";
            default -> "작업을 안전하게 적용할 수 없습니다.";
        };
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

    private LayoutResponse snapshotResponse(Layout layout) {
        Room room = roomAccessService.findReadableRoom(layout.getRoomId());
        AgentContext context = agentContextRepository.findById(layout.getContextId())
                .orElseThrow(() -> new CustomException(ErrorCode.CONTEXT_NOT_FOUND));
        ValidationResult validationResult = validationService.validate(room, layout.getFurniture());
        ScoreSummary scoreSummary = scoreService.calculate(context, layout.getFurniture(), validationResult);
        return LayoutResponse.ofSnapshot(layout, scoreSummary, validationResult);
    }

    private List<Furniture> deepCopyFurniture(List<Furniture> furniture) {
        return furniture.stream()
                .map(item -> copyFurniture(item, item.getPosition(), item.getRotation(),
                        item.getWidth(), item.getDepth(), item.getHeight(), item.getStatus()))
                .collect(Collectors.toCollection(ArrayList::new));
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

        FurnitureBoundary.Footprint footprint = FurnitureBoundary.footprint(
                furniture.getWidth(), furniture.getDepth(), override.getRotation(), furniture.getVariantId());
        Position position = new Position(override.getPosition().getX(), override.getPosition().getZ());
        if (!FurnitureBoundary.isInside(room, position, footprint)) {
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
                furniture.getDepth(), furniture.getRotation(), furniture.getVariantId());

        return copyFurniture(furniture, position, furniture.getRotation(), width,
                furniture.getDepth(), furniture.getHeight(), furniture.getStatus());
    }

    private Position clampPositionInsideRoom(Room room, Position position, double width,
                                             double depth, double rotation, String variantId) {
        FurnitureBoundary.Footprint footprint = FurnitureBoundary.footprint(width, depth, rotation, variantId);
        return FurnitureBoundary.clamp(room, position, footprint).orElse(position);
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
