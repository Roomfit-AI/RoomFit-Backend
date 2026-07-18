package com.roomfit.placement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.agent.domain.AgentContext;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.llm.LlmClient;
import com.roomfit.room.Furniture;
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
        if (feedback == null || feedback.isBlank()) {
            throw new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
        }

        String rawResponse;
        try {
            rawResponse = llmClient.complete(buildPrompt(feedback, room, furniture, context));
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
            return plan;
        } catch (CustomException e) {
            // Invalid provider output is a provider failure. The caller may safely use the
            // existing rule-based interpreter without making a second LLM request.
            throw new LlmProviderException(e);
        }
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
                    parsePlacement(item.path("placement")),
                    parseConstraints(item.path("constraints"), target.furnitureType(), feedback),
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
                text(node, "furnitureType"),
                text(node, "labelKeyword")
        );
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
                optionalEnum(FeedbackOrientation.class, text(node, "orientation"))
        );
    }

    private FeedbackClarification parseClarification(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.isObject()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        return new FeedbackClarification(text(node, "question"));
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

    private String buildPrompt(String feedback, Room room, List<Furniture> furniture, AgentContext context) {
        Map<String, Object> payload = Map.of(
                "feedback", feedback,
                "room", Map.of("width", room.getWidth(), "depth", room.getDepth(), "height", room.getHeight(),
                        "openings", room.getOpenings().stream().map(this::opening).toList()),
                "furniture", furniture.stream().map(this::furniture).toList(),
                "agentContext", Map.of("lifestyleGoal", context.getLifestyleGoal().name(),
                        "designStyle", context.getDesignStyle().stream().map(Enum::name).toList(),
                        "preferredColorTone", context.getPreferredColorTone() == null ? "" : context.getPreferredColorTone().name())
        );
        try {
            return """
                    Interpret Korean room-layout feedback into Plan v2 JSON only.
                    The only executable operation types are MOVE, ROTATE, and REPLACE_PRODUCT.
                    Use requestKind DIRECT for exactly one operation, COMPOSITE for two to four operations,
                    or CLARIFICATION with no operations when the target or request is ambiguous.
                    Each operation must have a unique operationId, a target selector, and dependsOn containing only earlier operationIds.
                    Target fields are furnitureId, furnitureType, and labelKeyword. Use only the fields needed to identify one item.
                    MOVE uses placement.relation from LEFT, RIGHT, FORWARD, BACKWARD, NEAR_WALL, NEAR_WINDOW,
                    AWAY_FROM_DOOR, CENTER and placement.magnitude from SMALL, MEDIUM, LARGE.
                    ROTATE uses placement.orientation from QUARTER_TURN_CW, QUARTER_TURN_CCW, HALF_TURN, ALIGN_WITH_WALL.
                    REPLACE_PRODUCT uses constraints with the supported fields furnitureType, largerThanCurrent, minWidth,
                    requiredStyleTags, requiredLifestyleTags, and storagePreferred.
                    Never output x, z, coordinates, position, distanceMeters, rotation, rotationDegrees, score,
                    validationResult, weight, objectiveWeight, productId, or variantId.
                    Do not output ADD_FURNITURE, REMOVE_FURNITURE, SWAP_FURNITURE, CHANGE_MATERIAL,
                    CHANGE_COLOR_TONE, ABSTRACT goals, coordinates, angles, scores, or validation decisions.
                    Return exactly this shape and no markdown or explanation:
                    {"version":"2.0","requestKind":"DIRECT","operations":[{"operationId":"op-1","type":"MOVE","target":{"furnitureId":"desk-1","furnitureType":"desk","labelKeyword":""},"placement":{"relation":"RIGHT","magnitude":"MEDIUM"},"constraints":null,"dependsOn":[]}],"goals":[],"clarification":null,"reason":"..."}
                    For CLARIFICATION, return operations=[], goals=[], and clarification={"question":"..."}.
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
                "rotation", item.getRotation(), "productId", item.getProductId() == null ? "" : item.getProductId(),
                "variantId", item.getVariantId() == null ? "" : item.getVariantId());
    }

    private Map<String, Object> opening(Opening opening) {
        return Map.of("type", opening.getType(), "wall", opening.getWall(), "offset", opening.getOffset(), "width", opening.getWidth());
    }

    private String text(JsonNode node, String field) {
        return node.path(field).isTextual() ? node.path(field).asText().trim() : "";
    }

    private Double optionalNumber(JsonNode node, String field) {
        return node.path(field).isNumber() ? node.path(field).asDouble() : null;
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
