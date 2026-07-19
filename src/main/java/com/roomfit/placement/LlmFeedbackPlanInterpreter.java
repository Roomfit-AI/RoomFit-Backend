package com.roomfit.placement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.agent.domain.AgentContext;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.llm.LlmClient;
import com.roomfit.product.catalog.GeneratedFurnitureCatalog;
import com.roomfit.room.Furniture;
import com.roomfit.room.FurnitureStatus;
import com.roomfit.room.Opening;
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
        String selection = selectedFurnitureId == null ? "" : selectedFurnitureId.trim();

        String rawResponse;
        try {
            rawResponse = llmClient.complete(buildPrompt(feedback, room, furniture, context, selection));
        } catch (RuntimeException e) {
            throw new LlmProviderException(e);
        }

        try {
            JsonNode root = parseObject(rawResponse);
            planValidator.validateProviderResponse(root);
            FeedbackPlan plan = new FeedbackPlan(
                    text(root, "version"),
                    enumValue(FeedbackRequestKind.class, text(root, "requestKind")),
                    parseOperations(root.path("operations"), feedback),
                    stringList(root.path("goals")),
                    parseClarification(root.path("clarification")),
                    text(root, "reason"),
                    FeedbackSource.LLM,
                    false
            );
            planValidator.validate(plan);
            validateProviderTargets(plan, furniture, feedback, selection);
            return plan;
        } catch (CustomException e) {
            // Invalid provider output is a provider failure. The caller may safely use the
            // existing rule-based interpreter without making a second LLM request.
            throw new LlmProviderException(e);
        }
    }

    /**
     * A concrete provider id is only a transport detail, never evidence that
     * the user selected that furniture.  With duplicate active canonical
     * types, require an actual user-visible discriminator or an exact UI
     * selection before the executor can see the operation.
     */
    private void validateProviderTargets(FeedbackPlan plan, List<Furniture> furniture, String feedback,
                                         String selectedFurnitureId) {
        for (FeedbackOperation operation : plan.operations()) {
            if (operation.type() != FeedbackOperationType.ADD_FURNITURE) {
                validateProviderTarget(operation.target(), furniture, feedback, selectedFurnitureId);
            }
            if (operation.referenceTarget() != null) {
                validateProviderTarget(operation.referenceTarget(), furniture, feedback, "");
            }
        }
    }

    private void validateProviderTarget(FeedbackTargetSelector selector, List<Furniture> furniture, String feedback,
                                        String selectedFurnitureId) {
        List<Furniture> active = furniture.stream()
                .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .toList();
        if (!selector.furnitureId().isBlank()) {
            Furniture matched = active.stream().filter(item -> selector.furnitureId().equals(item.getId()))
                    .findFirst().orElseThrow(() -> new CustomException(ErrorCode.INVALID_REQUEST_BODY));
            String matchedType = normalizeFurnitureType(matched.getType());
            if (!selector.furnitureType().isBlank() && !selector.furnitureType().equals(matchedType)) {
                throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
            }
            if (!selectedFurnitureId.isBlank() && !selectedFurnitureId.equals(matched.getId())) {
                throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
            }
        }

        String canonicalType = selector.furnitureType();
        if (canonicalType.isBlank() && !selector.furnitureId().isBlank()) {
            canonicalType = active.stream().filter(item -> selector.furnitureId().equals(item.getId()))
                    .map(item -> normalizeFurnitureType(item.getType())).findFirst().orElse("");
        }
        String targetType = canonicalType;
        long sameType = active.stream().map(item -> normalizeFurnitureType(item.getType()))
                .filter(targetType::equals).count();
        if (sameType > 1 && selectedFurnitureId.isBlank() && !hasUserDiscriminator(selector, feedback)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
    }

    private boolean hasUserDiscriminator(FeedbackTargetSelector selector, String feedback) {
        if (feedback == null || feedback.isBlank()) return false;
        String compact = feedback.replaceAll("\\s+", "").trim().toLowerCase(java.util.Locale.ROOT);
        if (!selector.labelKeyword().isBlank() && compact.contains(selector.labelKeyword().toLowerCase(java.util.Locale.ROOT))) {
            return true;
        }
        if (selector.ordinal() != null) {
            return compact.contains(selector.ordinal() + "번째")
                    || compact.contains(toKoreanOrdinal(selector.ordinal()) + " 번째");
        }
        if (selector.locationHint() == null) return false;
        return switch (selector.locationHint()) {
            case NEAR_WINDOW -> compact.contains("창가") || compact.contains("창문");
            case CENTER -> compact.contains("가운데") || compact.contains("중앙");
            case LARGEST -> compact.contains("가장 큰") || compact.contains("큰 ");
            case SMALLEST -> compact.contains("가장 작은") || compact.contains("작은 ");
        };
    }

    private String toKoreanOrdinal(int ordinal) {
        return switch (ordinal) {
            case 1 -> "첫";
            case 2 -> "두";
            case 3 -> "세";
            case 4 -> "네";
            default -> String.valueOf(ordinal);
        };
    }

    private List<FeedbackOperation> parseOperations(JsonNode node, String feedback) {
        if (!node.isArray()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        List<FeedbackOperation> operations = new ArrayList<>();
        for (JsonNode item : node) {
            FeedbackOperationType type = enumValue(FeedbackOperationType.class, text(item, "type"));
            FeedbackTargetSelector target = parseTarget(item.path("target"));
            operations.add(new FeedbackOperation(
                    text(item, "operationId"),
                    type,
                    target,
                    parseTargetOrNull(item.path("referenceTarget")),
                    parsePlacement(item.path("placement")),
                    parseConstraints(item.path("constraints"), target.furnitureType(), feedback),
                    parseProductRequirements(item.path("productRequirements"), target.furnitureType()),
                    parseProductRequirements(item.path("replacementRequirements"), ""),
                    stringList(item.path("dependsOn"))
            ));
        }
        return operations;
    }

    private FeedbackTargetSelector parseTarget(JsonNode node) {
        if (!node.isObject()) {
            return new FeedbackTargetSelector("", "", "");
        }
        return new FeedbackTargetSelector(
                text(node, "furnitureId"),
                normalizeFurnitureType(text(node, "furnitureType")),
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
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            if (root == null || !root.isObject()) {
                throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
            }
            return root;
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
    }

    private String buildPrompt(String feedback, Room room, List<Furniture> furniture, AgentContext context,
                               String selectedFurnitureId) {
        String activeSelectionId = furniture.stream()
                .filter(item -> item.getStatus() != FurnitureStatus.DELETED)
                .map(Furniture::getId)
                .filter(selectedFurnitureId::equals)
                .findFirst().orElse("");
        Map<String, Object> payload = Map.of(
                "feedback", feedback,
                "selectedFurnitureId", activeSelectionId,
                "room", Map.of("width", room.getWidth(), "depth", room.getDepth(), "height", room.getHeight(),
                        "openings", room.getOpenings().stream().map(this::opening).toList()),
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
                    AWAY_FROM_DOOR, CENTER and placement.magnitude from SMALL, MEDIUM, LARGE.
                    ROTATE uses placement.orientation from QUARTER_TURN_CW, QUARTER_TURN_CCW, HALF_TURN, ALIGN_WITH_WALL.
                    REPLACE_PRODUCT uses constraints with the supported fields furnitureType, largerThanCurrent, minWidth,
                    requiredStyleTags, requiredLifestyleTags, and storagePreferred.
                    ADD_FURNITURE describes the new type in target.furnitureType, uses productRequirements with
                    furnitureType, sizePreference (SMALL, LARGE, SIMILAR, ANY), storagePreferred, and styleKeywords,
                    and uses placement.relation from NEXT_TO, LEFT_OF, RIGHT_OF, NEAR_WALL, NEAR_WINDOW,
                    IN_CORNER, CENTER. NEXT_TO may use placement.side LEFT, RIGHT, FRONT, or BACK.
                    NEXT_TO, LEFT_OF, and RIGHT_OF require referenceTarget.
                    REMOVE_FURNITURE selects one existing target and has no placement or product requirements.
                    SWAP_FURNITURE selects one existing target and uses replacementRequirements with the same
                    product requirement fields. SWAP may change furnitureType; REPLACE_PRODUCT may not.
                    Use canonical furniture types bed, bookshelf, curtain_blind, desk, desk_chair, drawer_chest,
                    full_length_mirror, hanger, media_console, monitor, mood_lamp, multi_table, nightstand,
                    partition_shelf, plant, rug, side_table, sofa, sofa_bed, tv, and wardrobe. Treat lamp and
                    lighting as mood_lamp, chair as desk_chair, table as multi_table, and bedside table as nightstand.
                    Never output x, z, coordinates, position, distanceMeters, rotation, rotationDegrees, score,
                    validationResult, weight, objectiveWeight, productId, or variantId.
                    Do not output CHANGE_MATERIAL, CHANGE_COLOR_TONE, ABSTRACT goals, coordinates, angles,
                    scores, product identifiers, variant identifiers, or validation decisions.
                    Interpret add/place/insert expressions as ADD_FURNITURE, remove/take-out/delete expressions as
                    REMOVE_FURNITURE, and instead/replace/change-to expressions as SWAP_FURNITURE when the type changes.
                    Use semantic relations for beside/left/right/window/wall/corner expressions. If one existing target
                    or reference cannot be identified safely, return CLARIFICATION instead of guessing.
                    Return exactly this shape and no markdown or explanation:
                    {"version":"2.0","requestKind":"DIRECT","operations":[{"operationId":"op-1","type":"MOVE","target":{"furnitureId":"desk-1","furnitureType":"desk","labelKeyword":""},"placement":{"relation":"RIGHT","magnitude":"MEDIUM"},"constraints":null,"dependsOn":[]}],"goals":[],"clarification":null,"reason":"..."}
                    For CLARIFICATION, return operations=[], goals=[], and clarification={"question":"...",
                    "targetFurnitureType":"canonical type when the ambiguous target type is known"}. The question
                    is only explanatory; the backend constructs the final user-facing clarification response.
                    Existing room coordinates in the input are context only and must never be copied to the output.
                    Input:
                    %s
                    """.formatted(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
    }

    private Map<String, Object> furniture(Furniture item) {
        return Map.of("id", item.getId(), "type", item.getType(), "label", item.getLabel(),
                "width", item.getWidth(), "depth", item.getDepth(), "height", item.getHeight(),
                "position", Map.of("x", item.getPosition().getX(), "z", item.getPosition().getZ()),
                "rotation", item.getRotation());
    }

    private Map<String, Object> opening(Opening opening) {
        return Map.of("type", opening.getType(), "wall", opening.getWall(), "offset", opening.getOffset(), "width", opening.getWidth());
    }

    private String text(JsonNode node, String field) {
        return node.path(field).isTextual() ? node.path(field).asText().trim() : "";
    }

    private String normalizeFurnitureType(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return GeneratedFurnitureCatalog.get().normalizeType(value);
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
