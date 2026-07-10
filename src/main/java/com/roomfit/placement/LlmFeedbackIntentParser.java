package com.roomfit.placement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;
import com.roomfit.llm.LlmClient;

import java.util.LinkedHashMap;
import java.util.Map;

public class LlmFeedbackIntentParser implements FeedbackIntentParser {

    private static final double DEFAULT_DESK_MIN_WIDTH = 1.4;
    private static final double MIN_DESK_WIDTH = 1.0;
    private static final double MAX_DESK_WIDTH = 1.8;

    private final LlmClient llmClient;
    private final ObjectMapper objectMapper;

    public LlmFeedbackIntentParser(LlmClient llmClient) {
        this(llmClient, new ObjectMapper());
    }

    LlmFeedbackIntentParser(LlmClient llmClient, ObjectMapper objectMapper) {
        this.llmClient = llmClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public FeedbackIntent parse(String feedback) {
        if (feedback == null || feedback.isBlank()) {
            throw unsupportedFeedbackIntent();
        }

        JsonNode root = parseJson(llmClient.complete(buildPrompt(feedback)));
        String rawIntent = text(root, "intent");

        return switch (rawIntent) {
            case "ENLARGE_FURNITURE" -> parseEnlargeFurniture(root, rawIntent);
            case "INCREASE_STORAGE" -> storagePriority(root, rawIntent);
            case "MAKE_ROOM_SPACIOUS", "OPEN_SPACE_PRIORITY" -> openSpacePriority(root, rawIntent);
            default -> throw unsupportedFeedbackIntent();
        };
    }

    private FeedbackIntent parseEnlargeFurniture(JsonNode root, String rawIntent) {
        String targetFurniture = text(root, "targetFurniture");
        if (!"desk".equals(targetFurniture)) {
            throw unsupportedFeedbackIntent();
        }

        double minWidth = clamp(readMinWidth(root));
        Map<String, Object> constraints = new LinkedHashMap<>();
        constraints.put("minWidth", minWidth);

        Map<String, Object> interpretedIntent = baseInterpretedIntent(root, rawIntent);
        interpretedIntent.put("deskMinWidth", minWidth);
        interpretedIntent.put("targetFurniture", targetFurniture);
        interpretedIntent.put("constraints", constraints);
        interpretedIntent.put("fallbackUsed", false);
        return new FeedbackIntent(FeedbackIntentType.LARGER_DESK, interpretedIntent);
    }

    private FeedbackIntent storagePriority(JsonNode root, String rawIntent) {
        Map<String, Object> interpretedIntent = baseInterpretedIntent(root, rawIntent);
        interpretedIntent.put("storagePriority", "HIGH");
        interpretedIntent.put("fallbackUsed", false);
        return new FeedbackIntent(FeedbackIntentType.STORAGE_PRIORITY, interpretedIntent);
    }

    private FeedbackIntent openSpacePriority(JsonNode root, String rawIntent) {
        Map<String, Object> interpretedIntent = baseInterpretedIntent(root, rawIntent);
        interpretedIntent.put("openSpacePriority", "HIGH");
        interpretedIntent.put("fallbackUsed", false);
        return new FeedbackIntent(FeedbackIntentType.OPEN_SPACE_PRIORITY, interpretedIntent);
    }

    private Map<String, Object> baseInterpretedIntent(JsonNode root, String rawIntent) {
        Map<String, Object> interpretedIntent = new LinkedHashMap<>();
        interpretedIntent.put("source", "LLM");
        interpretedIntent.put("rawIntent", rawIntent);

        String targetFurniture = text(root, "targetFurniture");
        if (!targetFurniture.isBlank()) {
            interpretedIntent.put("targetFurniture", targetFurniture);
        }

        String priority = text(root, "priority");
        if (!priority.isBlank()) {
            interpretedIntent.put("priority", priority);
        }
        return interpretedIntent;
    }

    private JsonNode parseJson(String rawResponse) {
        if (rawResponse == null || rawResponse.isBlank()) {
            throw invalidRequestBody();
        }
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            if (root == null || !root.isObject()) {
                throw invalidRequestBody();
            }
            return root;
        } catch (JsonProcessingException e) {
            throw invalidRequestBody();
        }
    }

    private double readMinWidth(JsonNode root) {
        JsonNode minWidthNode = root.path("constraints").path("minWidth");
        if (minWidthNode.isNumber()) {
            return minWidthNode.asDouble();
        }
        if (minWidthNode.isTextual()) {
            try {
                return Double.parseDouble(minWidthNode.asText());
            } catch (NumberFormatException ignored) {
                return DEFAULT_DESK_MIN_WIDTH;
            }
        }
        return DEFAULT_DESK_MIN_WIDTH;
    }

    private double clamp(double value) {
        return Math.max(MIN_DESK_WIDTH, Math.min(MAX_DESK_WIDTH, value));
    }

    private String text(JsonNode root, String fieldName) {
        JsonNode node = root.path(fieldName);
        if (!node.isTextual()) {
            return "";
        }
        return node.asText().trim();
    }

    private String buildPrompt(String feedback) {
        return """
                Convert the user's room layout feedback into JSON only.
                Rules:
                - Do not generate x, z, rotation, coordinates, or placement values.
                - Choose exactly one supported intent: ENLARGE_FURNITURE, INCREASE_STORAGE, MAKE_ROOM_SPACIOUS, OPEN_SPACE_PRIORITY, or UNSUPPORTED.
                - Return JSON only. Do not include markdown or explanation.
                - If unsupported, return {"intent":"UNSUPPORTED"}.
                - For ENLARGE_FURNITURE, include targetFurniture and constraints.minWidth only when relevant.

                User feedback:
                %s
                """.formatted(feedback);
    }

    private CustomException unsupportedFeedbackIntent() {
        return new CustomException(ErrorCode.UNSUPPORTED_FEEDBACK_INTENT);
    }

    private CustomException invalidRequestBody() {
        return new CustomException(ErrorCode.INVALID_REQUEST_BODY);
    }
}
