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

    private static final double MAX_DISTANCE_METERS = 2.0;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public LlmFeedbackPlanInterpreter(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
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
            if (!"1.0".equals(text(root, "version"))) {
                throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
            }

            JsonNode target = root.path("target");
            String furnitureId = text(target, "furnitureId");
            String furnitureType = text(target, "furnitureType");
            List<FeedbackOperation> operations = parseOperations(root.path("operations"));
            return new FeedbackPlan("1.0", furnitureId, furnitureType, operations,
                    text(root, "reason"), FeedbackSource.LLM, false);
        } catch (CustomException e) {
            // A syntactically malformed or schema-invalid provider response is a provider failure,
            // not a reason to execute an invented layout change.
            throw new LlmProviderException(e);
        }
    }

    private List<FeedbackOperation> parseOperations(JsonNode node) {
        if (!node.isArray()) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        List<FeedbackOperation> operations = new ArrayList<>();
        for (JsonNode item : node) {
            String rawType = text(item, "type");
            if (!isMvpOperation(rawType)) {
                // Keep the response as an explicit no-op/unsupported result.  It must not be
                // converted into a rule-based operation.
                return List.of();
            }
            FeedbackOperationType type = enumValue(FeedbackOperationType.class, rawType);
            FeedbackDirection direction = optionalEnum(FeedbackDirection.class, text(item, "direction"));
            Double distance = optionalNumber(item, "distanceMeters");
            Integer rotation = optionalInteger(item, "rotationDegrees");
            FeedbackReplaceConstraints constraints = parseConstraints(item.path("constraints"));

            if (type == FeedbackOperationType.MOVE && (direction == null || distance == null
                    || distance <= 0 || distance > MAX_DISTANCE_METERS)) {
                throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
            }
            if (type == FeedbackOperationType.ROTATE && (rotation == null || rotation == 0 || Math.abs(rotation) > 360)) {
                throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
            }
            if (type == FeedbackOperationType.REPLACE_PRODUCT && constraints == null) {
                throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
            }
            operations.add(new FeedbackOperation(type, direction, distance, rotation, constraints));
        }
        return operations;
    }

    private boolean isMvpOperation(String rawType) {
        return FeedbackOperationType.MOVE.name().equals(rawType)
                || FeedbackOperationType.ROTATE.name().equals(rawType)
                || FeedbackOperationType.REPLACE_PRODUCT.name().equals(rawType);
    }

    private FeedbackReplaceConstraints parseConstraints(JsonNode node) {
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        Double minWidth = optionalNumber(node, "minWidth");
        if (minWidth != null && (minWidth <= 0 || minWidth > 10)) {
            throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
        }
        return new FeedbackReplaceConstraints(text(node, "furnitureType"),
                node.path("largerThanCurrent").asBoolean(false), minWidth,
                stringList(node.path("requiredStyleTags")), stringList(node.path("requiredLifestyleTags")),
                node.path("storagePreferred").asBoolean(false));
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
                    Interpret room layout feedback into JSON only. Do not produce coordinates or product IDs.
                    Return exactly: {"version":"1.0","target":{"furnitureId":"...","furnitureType":"..."},"operations":[...],"reason":"..."}.
                    Allowed operation types: MOVE, ROTATE, REPLACE_PRODUCT, ADD_FURNITURE, REMOVE_FURNITURE.
                    MOVE needs direction (LEFT, RIGHT, FORWARD, BACKWARD, NEAR_WALL, NEAR_WINDOW, AWAY_FROM_DOOR, CENTER) and distanceMeters in (0,2].
                    ROTATE needs rotationDegrees in [-360,360], excluding 0. REPLACE_PRODUCT needs constraints; never include productId.
                    If no furniture can be identified, return target with empty furnitureId and an empty operations array only when clarification is required.
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

    private Integer optionalInteger(JsonNode node, String field) {
        return node.path(field).canConvertToInt() ? node.path(field).asInt() : null;
    }

    private List<String> stringList(JsonNode node) {
        if (!node.isArray() || node.isEmpty()) return List.of();
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (!item.isTextual() || item.asText().isBlank()) throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
            values.add(item.asText());
        }
        return values;
    }

    private <T extends Enum<T>> T enumValue(Class<T> type, String raw) {
        try { return Enum.valueOf(type, raw); }
        catch (IllegalArgumentException e) { throw new CustomException(ErrorCode.INVALID_REQUEST_BODY); }
    }

    private <T extends Enum<T>> T optionalEnum(Class<T> type, String raw) {
        return raw.isBlank() ? null : enumValue(type, raw);
    }
}
