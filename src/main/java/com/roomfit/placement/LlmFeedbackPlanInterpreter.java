package com.roomfit.placement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.agent.domain.AgentContext;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.llm.LlmClient;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class LlmFeedbackPlanInterpreter implements FeedbackPlanInterpreter {

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;
    private final FeedbackPlanValidator planValidator;

    public LlmFeedbackPlanInterpreter(LlmClient llmClient, ObjectMapper objectMapper) {
        this(llmClient, objectMapper, new FeedbackPlanValidator());
    }

    LlmFeedbackPlanInterpreter(LlmClient llmClient, ObjectMapper objectMapper,
                               FeedbackPlanValidator planValidator) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
        this.planValidator = planValidator;
    }

    @Override
    public FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context) {
        return interpret(feedback, room, furniture, context, "");
    }

    @Override
    public FeedbackPlan interpret(String feedback, Room room, List<Furniture> furniture, AgentContext context,
                                  String selectedFurnitureId) {
        if (feedback == null || feedback.isBlank()) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
        }

        String rawResponse;
        try {
            rawResponse = llmClient.complete(buildPrompt(feedback, room, furniture, context, selectedFurnitureId));
        } catch (RuntimeException e) {
            throw new LlmProviderException(LlmProviderException.PROVIDER_CALL, e);
        }

        JsonNode root = parseObject(rawResponse);
        try {
            planValidator.validateProviderResponse(root);
        } catch (CustomException e) {
            throw new LlmProviderException(LlmProviderException.OTHER_SAFETY_POLICY, e);
        }

        FeedbackPlan plan;
        try {
            plan = new FeedbackPlan(
                    text(root, "version"),
                    enumValue(FeedbackRequestKind.class, text(root, "requestKind")),
                    parseOperations(root.path("operations"), feedback),
                    stringList(root.path("goals")),
                    parseClarification(root.path("clarification")),
                    text(root, "reason"),
                    FeedbackSource.LLM,
                    false
            );
        } catch (CustomException e) {
            throw new LlmProviderException(LlmProviderException.PLAN_SCHEMA_OR_ENUM, e);
        }

        try {
            planValidator.validate(plan);
        } catch (CustomException e) {
            throw new LlmProviderException(semanticValidationStage(plan), e);
        }

        try {
            plan = normalizeImplicitProviderAdds(plan, feedback, furniture, selectedFurnitureId);
            planValidator.validate(plan);
        } catch (CustomException e) {
            throw new LlmProviderException(LlmProviderException.OTHER_SAFETY_POLICY, e);
        }
        try {
            validateProviderTargets(plan, furniture, selectedFurnitureId, feedback);
        } catch (CustomException e) {
            // Invalid provider output is a provider failure. The caller may safely use the
            // existing rule-based interpreter without making a second LLM request.
            throw new LlmProviderException(LlmProviderException.SEMANTIC_TARGET_REFERENCE_UNRESOLVED, e);
        }
        return plan;
    }

    private String semanticValidationStage(FeedbackPlan plan) {
        if (plan.operations().size() > FeedbackPlanValidator.MAX_FEEDBACK_OPERATIONS) {
            return LlmProviderException.SEMANTIC_OPERATION_LIMIT;
        }
        for (FeedbackOperation operation : plan.operations()) {
            if (operation.target() == null || operation.target().isEmpty()) {
                return LlmProviderException.SEMANTIC_TARGET_EMPTY;
            }
            FeedbackTargetSelector reference = operation.referenceTarget();
            if (reference != null && (reference.isEmpty() || sameTarget(operation.target(), reference))) {
                return LlmProviderException.SEMANTIC_REFERENCE_EMPTY_OR_SAME;
            }
            if (operation.type() == FeedbackOperationType.MOVE && operation.placement() != null) {
                FeedbackRelation relation = operation.placement().relation();
                boolean referenceRelation = relation == FeedbackRelation.NEXT_TO
                        || relation == FeedbackRelation.LEFT_OF || relation == FeedbackRelation.RIGHT_OF;
                if (reference != null && !referenceRelation) {
                    return LlmProviderException.SEMANTIC_REFERENCE_PRESENT_WITH_NON_REFERENCE_RELATION;
                }
                if (reference == null && referenceRelation) {
                    return LlmProviderException.SEMANTIC_REFERENCE_RELATION_WITHOUT_REFERENCE;
                }
            }
        }
        return LlmProviderException.SEMANTIC_OPERATION;
    }

    private boolean sameTarget(FeedbackTargetSelector target, FeedbackTargetSelector reference) {
        return !target.furnitureId().isBlank() && target.furnitureId().equals(reference.furnitureId());
    }

    private List<FeedbackOperation> parseOperations(JsonNode node, String feedback) {
        if (!node.isArray()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        List<FeedbackOperation> operations = new ArrayList<>();
        for (JsonNode item : node) {
            FeedbackOperationType type = enumValue(FeedbackOperationType.class, text(item, "type"));
            FeedbackTargetSelector target = parseTarget(item.path("target"));
            FeedbackProductRequirements replacementRequirements = parseProductRequirements(
                    item.path("replacementRequirements"), target.furnitureType());
            if (type == FeedbackOperationType.SWAP_FURNITURE
                    && FeedbackMetadataKeywordNormalizer.containsMetadataRequest(feedback)) {
                List<String> metadataKeywords = FeedbackMetadataKeywordNormalizer.keywordsFor(feedback);
                if (metadataKeywords.isEmpty() || replacementRequirements == null
                        || !target.furnitureType().equals(replacementRequirements.furnitureType())) {
                    throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
                }
                replacementRequirements = new FeedbackProductRequirements(target.furnitureType(),
                        replacementRequirements.sizePreference(), replacementRequirements.storagePreferred(), metadataKeywords);
            }
            operations.add(new FeedbackOperation(
                    text(item, "operationId"),
                    type,
                    target,
                    parseTargetOrNull(item.path("referenceTarget")),
                    parsePlacement(item.path("placement")),
                    parseConstraints(item.path("constraints"), target.furnitureType(), feedback),
                    parseProductRequirements(item.path("productRequirements"), target.furnitureType()),
                    replacementRequirements,
                    stringList(item.path("dependsOn"))
            ));
        }
        return operations;
    }

    private FeedbackTargetSelector parseTarget(JsonNode node) {
        if (!node.isObject()) {
            return new FeedbackTargetSelector("", "", "");
        }
        String rawType = text(node, "furnitureType");
        String furnitureType = normalizeFurnitureType(rawType);
        if (!rawType.isBlank() && furnitureType.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        return new FeedbackTargetSelector(
                text(node, "furnitureId"),
                furnitureType,
                text(node, "labelKeyword"),
                optionalEnum(FeedbackLocationHint.class, text(node, "locationHint")),
                optionalInteger(node, "ordinal")
        );
    }

    private FeedbackTargetSelector parseTargetOrNull(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return parseTarget(node);
    }

    private FeedbackPlacement parsePlacement(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        return new FeedbackPlacement(
                optionalEnum(FeedbackRelation.class, text(node, "relation")),
                optionalEnum(FeedbackMagnitude.class, text(node, "magnitude")),
                optionalEnum(FeedbackOrientation.class, text(node, "orientation")),
                optionalEnum(FeedbackSide.class, text(node, "side"))
        );
    }

    private FeedbackProductRequirements parseProductRequirements(JsonNode node, String defaultFurnitureType) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        String furnitureType = text(node, "furnitureType");
        if (furnitureType.isBlank()) {
            furnitureType = defaultFurnitureType;
        }
        furnitureType = normalizeFurnitureType(furnitureType);
        if (furnitureType.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        return new FeedbackProductRequirements(
                furnitureType,
                optionalEnum(FeedbackSizePreference.class, text(node, "sizePreference")),
                node.path("storagePreferred").asBoolean(false)
                        || node.path("storageRequired").asBoolean(false),
                stringList(node.path("styleKeywords"))
        );
    }

    private FeedbackClarification parseClarification(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        return new FeedbackClarification(text(node, "question"),
                normalizeFurnitureType(text(node, "targetFurnitureType")));
    }

    private FeedbackReplaceConstraints parseConstraints(JsonNode node, String targetFurnitureType, String feedback) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        String furnitureType = text(node, "furnitureType");
        if (furnitureType.isBlank()) {
            furnitureType = targetFurnitureType;
        }
        furnitureType = normalizeFurnitureType(furnitureType);
        if (furnitureType.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        boolean largerThanCurrent = node.path("largerThanCurrent").asBoolean(false);
        Double minWidth = optionalNumber(node, "minWidth");
        if (minWidth != null && (minWidth <= 0 || minWidth > 10)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        boolean storagePreferred = node.path("storagePreferred").asBoolean(false)
                || node.path("storageRequired").asBoolean(false);

        boolean storageRequest = isStorageRequest(feedback);
        boolean largerRequest = isLargerRequest(feedback);
        if (storageRequest && !largerRequest) {
            storagePreferred = true;
            largerThanCurrent = false;
            minWidth = null;
        } else if (largerRequest) {
            largerThanCurrent = true;
        }

        return new FeedbackReplaceConstraints(furnitureType,
                largerThanCurrent, minWidth,
                stringList(node.path("requiredStyleTags")), stringList(node.path("requiredLifestyleTags")),
                storagePreferred);
    }

    private boolean isStorageRequest(String feedback) {
        return feedback != null && feedback.contains("수납");
    }

    private boolean isLargerRequest(String feedback) {
        if (feedback == null) {
            return false;
        }
        return feedback.contains("넓")
                || feedback.contains("크게")
                || feedback.contains("키워")
                || feedback.contains("컸으면")
                || feedback.contains("큰 책상");
    }

    private JsonNode parseObject(String rawResponse) {
        String jsonObject;
        try {
            jsonObject = extractJsonObject(rawResponse);
        } catch (CustomException e) {
            throw new LlmProviderException(LlmProviderException.RESPONSE_NOT_JSON_OBJECT, e);
        }
        try {
            JsonNode root = objectMapper.readTree(jsonObject);
            if (root == null || !root.isObject()) {
                throw new LlmProviderException(LlmProviderException.RESPONSE_NOT_JSON_OBJECT, null);
            }
            return root;
        } catch (JsonProcessingException e) {
            throw new LlmProviderException(LlmProviderException.JSON_PARSE, e);
        }
    }

    /** Accept harmless presentation wrappers but never repair a malformed plan. */
    private String extractJsonObject(String rawResponse) {
        String value = rawResponse == null ? "" : rawResponse.trim();
        if (value.startsWith("```")) {
            int firstLineEnd = value.indexOf('\n');
            int closingFence = value.lastIndexOf("```");
            if (firstLineEnd < 0 || closingFence <= firstLineEnd) {
                throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
            }
            value = value.substring(firstLineEnd + 1, closingFence).trim();
        }
        if (value.startsWith("{") && value.endsWith("}")) {
            return value;
        }
        int first = value.indexOf('{');
        int last = value.lastIndexOf('}');
        if (first < 0 || last <= first || first > 160 || value.length() - last - 1 > 160) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        return value.substring(first, last + 1);
    }

    private String buildPrompt(String feedback, Room room, List<Furniture> furniture, AgentContext context,
                               String selectedFurnitureId) {
        String activeSelectionId = furniture.stream()
                .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .map(Furniture::getId)
                .filter(id -> id.equals(selectedFurnitureId))
                .findFirst().orElse("");
        Map<String, Object> payload = Map.of(
                "feedback", feedback,
                "selectedFurnitureId", activeSelectionId,
                "room", Map.of("hasWindow", room.getOpenings().stream()
                        .anyMatch(opening -> "window".equals(opening.getType()))),
                "furniture", furniture.stream()
                        .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                        .map(this::furniture)
                        .toList(),
                "agentContext", Map.of("lifestyleGoal", context.getLifestyleGoal().name(),
                        "designStyle", context.getDesignStyle().stream().map(Enum::name).toList(),
                        "preferredColorTone", context.getPreferredColorTone() == null ? "" : context.getPreferredColorTone().name())
        );
        try {
            return """
                    Interpret Korean room-layout feedback into Plan v2 JSON only.
                    The only executable operation types are MOVE, ROTATE, REPLACE_PRODUCT,
                    ADD_FURNITURE, REMOVE_FURNITURE, and SWAP_FURNITURE.
                    Use requestKind DIRECT for exactly one operation, COMPOSITE for two to four operations,
                    or CLARIFICATION with no operations when the target or request is ambiguous.
                    Each operation must have a unique operationId, a target selector, and dependsOn containing only earlier operationIds.
                    Target and referenceTarget fields are furnitureId, furnitureType, labelKeyword, locationHint,
                    and ordinal. Use only the fields needed to identify one item. locationHint may be NEAR_WINDOW,
                    CENTER, LARGEST, or SMALLEST. ordinal is one-based.
                    MOVE uses placement.relation from LEFT, RIGHT, FORWARD, BACKWARD, NEAR_WALL, NEAR_WINDOW,
                    AWAY_FROM_DOOR, CENTER with magnitude SMALL, MEDIUM, or LARGE. MOVE may also use IN_CORNER
                    without magnitude, or NEXT_TO, LEFT_OF, RIGHT_OF with referenceTarget and without magnitude.
                    Only NEXT_TO may use placement.side LEFT, RIGHT, FRONT, or BACK. Do not convert a MOVE into ADD.
                    For "A를 B 가까이/옆에/주변에 옮겨줘", target is A, referenceTarget is B, and
                    placement.relation is NEXT_TO with no magnitude. For example, "모니터를 책상 가까이 옮겨줘"
                    must use this JSON operation shape (choose only active supplied selectors; never invent IDs):
                    {"operationId":"op-1","type":"MOVE","target":{"furnitureType":"monitor"},"referenceTarget":{"furnitureType":"desk"},"placement":{"relation":"NEXT_TO"},"dependsOn":[]}.
                    If referenceTarget is present, placement.relation MUST be NEXT_TO, LEFT_OF, or RIGHT_OF.
                    Conversely, NEXT_TO, LEFT_OF, and RIGHT_OF require referenceTarget. NEAR_WALL, NEAR_WINDOW,
                    IN_CORNER, CENTER, LEFT, RIGHT, FORWARD, and BACKWARD must not include referenceTarget.
                    target and referenceTarget must identify different existing furniture.
                    ROTATE uses placement.orientation from QUARTER_TURN_CW, QUARTER_TURN_CCW, HALF_TURN, ALIGN_WITH_WALL.
                    REPLACE_PRODUCT uses constraints with the supported fields furnitureType, largerThanCurrent, minWidth,
                    requiredStyleTags, requiredLifestyleTags, and storagePreferred.
                    ADD_FURNITURE describes the new type in target.furnitureType, uses productRequirements with
                    furnitureType, sizePreference (SMALL, LARGE, SIMILAR, ANY), storagePreferred, and styleKeywords,
                    and uses placement.relation from NEXT_TO, LEFT_OF, RIGHT_OF, NEAR_WALL, NEAR_WINDOW,
                    IN_CORNER, CENTER. NEXT_TO may use placement.side LEFT, RIGHT, FRONT, or BACK.
                    REMOVE_FURNITURE selects one existing target and has no placement or product requirements.
                    SWAP_FURNITURE selects one existing target and uses replacementRequirements with the same
                    product requirement fields. A tone or material change must keep the target canonical type.
                    For Korean wood/natural-material terms use styleKeywords ["wood"], for bright/white terms use
                    ["paintedWhite"], and for metal terms use ["metal"]. These are catalog material values, not
                    invented product identifiers; if no single safe matching candidate exists, return CLARIFICATION.
                    Use canonical furniture types bed, bookshelf, curtain_blind, desk, desk_chair, drawer_chest,
                    full_length_mirror, hanger, media_console, monitor, mood_lamp, multi_table, nightstand,
                    partition_shelf, plant, rug, side_table, sofa, sofa_bed, tv, and wardrobe. Treat lamp and
                    lighting as mood_lamp, chair as desk_chair, table as multi_table, and bedside table as nightstand.
                    Never output x, z, coordinates, position, distanceMeters, rotation, rotationDegrees, score,
                    validationResult, weight, objectiveWeight, productId, or variantId.
                    Do not output CHANGE_MATERIAL, CHANGE_COLOR_TONE, ABSTRACT goals, coordinates, angles,
                    scores, product identifiers, variant identifiers, or validation decisions.
                    Use ADD_FURNITURE only for an explicit furniture-count increase: Korean 추가, 하나 더, 한 개 더,
                    새로 추가, or 새 가구를 추가. Place expressions such as 넣어, 배치해줘, 두어, 놓아, 놔, and
                    있었으면 좋겠 are not add signals by themselves. If an active item of that type already exists and the
                    user says to place it without an add signal, interpret it as MOVE.
                    Interpret remove/take-out/delete expressions as REMOVE_FURNITURE,
                    and replace/change expressions as SWAP_FURNITURE. A same-type "different design" request is SWAP_FURNITURE.
                    Use semantic relations for beside/left/right/window/wall/corner expressions. If one existing target
                    or reference cannot be identified safely, return CLARIFICATION instead of guessing.
                    When selectedFurnitureId is present and the user uses a generic target such as "가구" or "저거",
                    use that exact active furniture as the MOVE target. For a type-omitted tone/material SWAP, use that
                    exact active furniture as the target. Do not use it to override an explicit furniture name.
                    Keep composite operations in the sentence order, emit at most four operations, and return one
                    CLARIFICATION plan rather than a partial plan if any clause is ambiguous or unsupported.
                    Return exactly this shape and no markdown or explanation:
                    {"version":"2.0","requestKind":"DIRECT","operations":[{"operationId":"op-1","type":"MOVE","target":{"furnitureId":"desk-1","furnitureType":"desk","labelKeyword":""},"placement":{"relation":"RIGHT","magnitude":"MEDIUM"},"constraints":null,"dependsOn":[]}],"goals":[],"clarification":null,"reason":"..."}
                    For CLARIFICATION, return operations=[], goals=[], and clarification={"question":"...",
                    "targetFurnitureType":"canonical type when the ambiguous target type is known"}. The question
                    is only explanatory; the backend constructs the final user-facing clarification response.
                    Do not invent furniture identifiers. Deleted furniture is not a target. If duplicate active items
                    cannot be distinguished by their supplied id, label, or a supported selector, return CLARIFICATION.
                    Input:
                    %s
                    """.formatted(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
    }

    private Map<String, Object> furniture(Furniture item) {
        return Map.of("id", item.getId(), "type", item.getType(), "label", item.getLabel(),
                "status", item.getStatus().name());
    }

    private String text(JsonNode node, String field) {
        return node.path(field).isTextual() ? node.path(field).asText().trim() : "";
    }

    private String normalizeFurnitureType(String value) {
        return FeedbackVocabularyNormalizer.normalizeCanonicalType(value);
    }

    /**
     * A provider may not turn a request to move, replace, or remove an existing
     * item into an ADD merely because it cannot resolve the target.  That is a
     * semantic safety rule, not a prompt preference, so enforce it after JSON
     * parsing as well.
     */
    private FeedbackPlan normalizeImplicitProviderAdds(FeedbackPlan plan, String feedback,
                                                       List<Furniture> furniture, String selectedFurnitureId) {
        if (FeedbackActionIntentResolver.hasExplicitFurnitureCreationIntent(feedback) || plan.operations().stream()
                .noneMatch(operation -> operation.type() == FeedbackOperationType.ADD_FURNITURE)) {
            return plan;
        }

        List<FeedbackOperation> normalized = new ArrayList<>();
        for (FeedbackOperation operation : plan.operations()) {
            if (operation.type() != FeedbackOperationType.ADD_FURNITURE) {
                normalized.add(operation);
                continue;
            }
            String type = requestedCanonicalType(feedback, furniture, selectedFurnitureId);
            if (type.isBlank()) {
                return providerClarification("");
            }
            List<Furniture> activeMatches = furniture.stream()
                    .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                    .filter(item -> type.equals(FeedbackVocabularyNormalizer.normalizeCanonicalType(item.getType())))
                    .toList();
            if (activeMatches.size() != 1) {
                return providerClarification(type);
            }
            Furniture existing = activeMatches.getFirst();
            normalized.add(new FeedbackOperation(operation.operationId(), FeedbackOperationType.MOVE,
                    new FeedbackTargetSelector(existing.getId(), type, ""),
                    null, implicitMovePlacement(feedback), null, null, null, operation.dependsOn()));
        }
        return new FeedbackPlan(plan.version(), plan.requestKind(), normalized, plan.goals(), plan.clarification(),
                plan.reason(), plan.source(), plan.fallbackUsed());
    }

    private FeedbackPlacement implicitMovePlacement(String feedback) {
        if (containsAny(feedback, List.of("구석", "모서리", "코너"))) {
            return new FeedbackPlacement(FeedbackRelation.IN_CORNER, null, null);
        }
        FeedbackRelation relation = containsAny(feedback, List.of("창가", "창문")) ? FeedbackRelation.NEAR_WINDOW
                : feedback.contains("벽") ? FeedbackRelation.NEAR_WALL
                : feedback.contains("왼쪽") ? FeedbackRelation.LEFT
                : feedback.contains("오른쪽") ? FeedbackRelation.RIGHT
                : containsAny(feedback, List.of("가운데", "중앙")) ? FeedbackRelation.CENTER
                : FeedbackRelation.NEAR_WALL;
        return new FeedbackPlacement(relation, FeedbackMagnitude.MEDIUM, null);
    }

    private FeedbackPlan providerClarification(String type) {
        return new FeedbackPlan("2.0", FeedbackRequestKind.CLARIFICATION, List.of(), List.of(),
                new FeedbackClarification("가구를 새로 추가할지 기존 가구를 옮길지 확인이 필요합니다.", type),
                "", FeedbackSource.LLM, false);
    }

    private boolean containsAny(String value, List<String> terms) {
        return terms.stream().anyMatch(value::contains);
    }

    private void validateProviderTargets(FeedbackPlan plan, List<Furniture> furniture, String selectedFurnitureId,
                                         String feedback) {
        List<CanonicalMention> mentions = canonicalMentions(feedback);
        for (int index = 0; index < plan.operations().size(); index++) {
            FeedbackOperation operation = plan.operations().get(index);
            String expectedTargetType = expectedOperationTargetType(plan, index, mentions, furniture, selectedFurnitureId,
                    feedback);
            if (expectedTargetType.isBlank()) {
                throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
            }
            validateProviderTarget(operation.target(), furniture, selectedFurnitureId, feedback, true, expectedTargetType);
            if (operation.referenceTarget() != null) {
                String expectedReferenceType = expectedReferenceTargetType(operation, mentions, furniture, selectedFurnitureId);
                if (expectedReferenceType.isBlank()) {
                    throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
                }
                validateProviderTarget(operation.referenceTarget(), furniture, selectedFurnitureId, feedback, false,
                        expectedReferenceType);
            }
        }
    }

    private void validateProviderTarget(FeedbackTargetSelector selector, List<Furniture> furniture,
                                        String selectedFurnitureId, String feedback, boolean isOperationTarget,
                                        String expectedType) {
        if (!expectedType.equals(selector.furnitureType())) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        // A type-only selector remains valid: the deterministic resolver already
        // turns zero or multiple active matches into a safe result.  IDs are
        // different because a fabricated ID could otherwise silently select a
        // different type through an overly broad fallback.
        if (selector.furnitureId().isBlank()) {
            return;
        }
        List<Furniture> active = furniture.stream()
                .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .filter(item -> selector.furnitureId().equals(item.getId()))
                .filter(item -> selector.furnitureType().isBlank()
                        || selector.furnitureType().equals(FeedbackVocabularyNormalizer.normalizeCanonicalType(item.getType())))
                .filter(item -> selector.labelKeyword().isBlank() || (item.getLabel() != null
                        && item.getLabel().toLowerCase(java.util.Locale.ROOT)
                        .contains(selector.labelKeyword().toLowerCase(java.util.Locale.ROOT))))
                .toList();
        if (active.size() != 1) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        boolean hasDiscriminator = !selector.labelKeyword().isBlank()
                || selector.locationHint() != null || selector.ordinal() != null;
        long sameTypeActiveCount = furniture.stream()
                .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .filter(item -> selector.furnitureType()
                        .equals(FeedbackVocabularyNormalizer.normalizeCanonicalType(item.getType())))
                .count();
        boolean selectedGenericTarget = isOperationTarget && selector.furnitureId().equals(selectedFurnitureId)
                && !explicitlyMentionsCanonicalType(feedback, selector.furnitureType());
        if (sameTypeActiveCount > 1 && !selectedGenericTarget && !hasDiscriminator) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
    }

    private boolean explicitlyMentionsCanonicalType(String feedback, String canonicalType) {
        if (feedback == null || feedback.isBlank()) {
            return false;
        }
        String compactFeedback = feedback.replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT);
        return compactFeedback.contains(canonicalType.toLowerCase(java.util.Locale.ROOT))
                || FeedbackVocabularyNormalizer.aliasesByLength().stream()
                .filter(entry -> canonicalType.equals(entry.getValue()))
                .map(entry -> entry.getKey().replace("_", ""))
                .anyMatch(compactFeedback::contains);
    }

    private String requestedCanonicalType(String feedback, List<Furniture> furniture, String selectedFurnitureId) {
        if (selectedFurnitureId != null && !selectedFurnitureId.isBlank()) {
            return furniture.stream()
                    .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                    .filter(item -> selectedFurnitureId.equals(item.getId()))
                    .map(item -> FeedbackVocabularyNormalizer.normalizeCanonicalType(item.getType()))
                    .findFirst().orElse("");
        }
        List<CanonicalMention> mentions = canonicalMentions(feedback);
        return mentions.size() == 1 ? mentions.getFirst().type() : "";
    }

    private String expectedOperationTargetType(FeedbackPlan plan, int operationIndex,
                                               List<CanonicalMention> mentions, List<Furniture> furniture,
                                               String selectedFurnitureId, String feedback) {
        FeedbackOperation operation = plan.operations().get(operationIndex);
        String selectedType = selectedCanonicalType(furniture, selectedFurnitureId);
        if (operation.type() != FeedbackOperationType.ADD_FURNITURE && !selectedType.isBlank()) {
            return selectedType;
        }
        if (mentions.size() == 1) {
            return mentions.getFirst().type();
        }
        if (mentions.isEmpty() && operation.type() == FeedbackOperationType.SWAP_FURNITURE
                && feedback.contains("수납장") && FeedbackMetadataKeywordNormalizer.containsMetadataRequest(feedback)) {
            return "drawer_chest";
        }
        if (isReferenceRelation(operation) && mentions.size() == 2) {
            return operation.type() == FeedbackOperationType.ADD_FURNITURE
                    ? mentions.get(1).type() : mentions.getFirst().type();
        }
        if (plan.operations().size() > 1 && mentions.size() == plan.operations().size()) {
            return mentions.get(operationIndex).type();
        }
        return "";
    }

    private String expectedReferenceTargetType(FeedbackOperation operation, List<CanonicalMention> mentions,
                                               List<Furniture> furniture, String selectedFurnitureId) {
        if (!isReferenceRelation(operation)) {
            return "";
        }
        String selectedType = selectedCanonicalType(furniture, selectedFurnitureId);
        if (!selectedType.isBlank()) {
            if (mentions.size() == 1 && !selectedType.equals(mentions.getFirst().type())) {
                return mentions.getFirst().type();
            }
            if (mentions.size() == 2) {
                return mentions.stream().map(CanonicalMention::type)
                        .filter(type -> !selectedType.equals(type)).distinct()
                        .reduce((first, second) -> "").orElse("");
            }
            return "";
        }
        if (mentions.size() != 2) {
            return "";
        }
        return operation.type() == FeedbackOperationType.ADD_FURNITURE
                ? mentions.getFirst().type() : mentions.get(1).type();
    }

    private boolean isReferenceRelation(FeedbackOperation operation) {
        if (operation.placement() == null) {
            return false;
        }
        FeedbackRelation relation = operation.placement().relation();
        return relation == FeedbackRelation.NEXT_TO || relation == FeedbackRelation.LEFT_OF
                || relation == FeedbackRelation.RIGHT_OF;
    }

    private String selectedCanonicalType(List<Furniture> furniture, String selectedFurnitureId) {
        if (selectedFurnitureId == null || selectedFurnitureId.isBlank()) {
            return "";
        }
        return furniture.stream().filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .filter(item -> selectedFurnitureId.equals(item.getId()))
                .map(item -> FeedbackVocabularyNormalizer.normalizeCanonicalType(item.getType()))
                .findFirst().orElse("");
    }

    private List<CanonicalMention> canonicalMentions(String feedback) {
        if (feedback == null || feedback.isBlank()) {
            return List.of();
        }
        String compact = feedback.replaceAll("\\s+", "").toLowerCase(java.util.Locale.ROOT);
        List<CanonicalMention> found = new ArrayList<>();
        for (Map.Entry<String, String> alias : FeedbackVocabularyNormalizer.aliasesByLength()) {
            String term = alias.getKey().replace("_", "");
            int from = 0;
            while (from < compact.length()) {
                int index = compact.indexOf(term, from);
                if (index < 0) {
                    break;
                }
                found.add(new CanonicalMention(alias.getValue(), index, term.length()));
                from = index + term.length();
            }
        }
        found.sort(java.util.Comparator.comparingInt(CanonicalMention::index)
                .thenComparing(java.util.Comparator.comparingInt(CanonicalMention::length).reversed()));
        List<CanonicalMention> nonOverlapping = new ArrayList<>();
        int end = -1;
        for (CanonicalMention mention : found) {
            if (mention.index() >= end) {
                nonOverlapping.add(mention);
                end = mention.index() + mention.length();
            }
        }
        return nonOverlapping;
    }

    private record CanonicalMention(String type, int index, int length) {
    }

    private Double optionalNumber(JsonNode node, String field) {
        return node.path(field).isNumber() ? node.path(field).asDouble() : null;
    }

    private Integer optionalInteger(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        if (!value.isIntegralNumber() || !value.canConvertToInt()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        return value.asInt();
    }

    private List<String> stringList(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual() || item.asText().isBlank()) {
                throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
            }
            values.add(item.asText().trim());
        }
        return values;
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String raw) {
        try {
            return Enum.valueOf(type, raw);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
    }

    private <T extends Enum<T>> T optionalEnum(Class<T> type, String raw) {
        return raw.isBlank() ? null : enumValue(type, raw);
    }
}
