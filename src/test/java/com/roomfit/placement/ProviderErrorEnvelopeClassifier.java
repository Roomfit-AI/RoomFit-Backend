package com.roomfit.placement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashSet;
import java.util.Set;

/** Test-only redaction boundary for live-evaluation HTTP error diagnostics. */
final class ProviderErrorEnvelopeClassifier {

    private static final Set<String> SAFE_FIELDS = Set.of(
            "model", "messages", "temperature", "response_format", "endpoint");

    private ProviderErrorEnvelopeClassifier() {
    }

    static String classify(String rawBody) {
        if (rawBody == null || rawBody.isBlank()) return "EMPTY_ERROR_BODY";
        final JsonNode root;
        try {
            root = new ObjectMapper().readTree(rawBody);
        } catch (Exception ignored) {
            return "NON_JSON_ERROR_BODY";
        }
        if (root == null || !root.isObject()) return "UNRECOGNIZED_ERROR_ENVELOPE";
        JsonNode error = root.path("error");
        if (!error.isObject()) return "UNRECOGNIZED_ERROR_ENVELOPE";

        String code = safeToken(error.path("code").asText());
        String status = safeToken(error.path("status").asText());
        String type = safeToken(error.path("type").asText());
        Set<String> fields = new LinkedHashSet<>();
        addSafeField(fields, error.path("param").asText());
        addSafeField(fields, error.path("field").asText());
        findFieldViolations(error.path("details"), fields);
        if (fields.isEmpty()) {
            String message = error.path("message").asText("");
            SAFE_FIELDS.stream().filter(message::contains).forEach(fields::add);
        }

        StringBuilder result = new StringBuilder("JSON_ERROR_ENVELOPE");
        append(result, "CODE", code);
        append(result, "STATUS", status);
        append(result, "TYPE", type);
        fields.stream().sorted().forEach(field -> append(result, "FIELD", field.toUpperCase(java.util.Locale.ROOT)));
        return result.toString();
    }

    private static void findFieldViolations(JsonNode node, Set<String> fields) {
        if (node == null || node.isMissingNode() || node.isNull()) return;
        if (node.isArray()) {
            node.forEach(item -> findFieldViolations(item, fields));
            return;
        }
        if (!node.isObject()) return;
        JsonNode violations = node.path("fieldViolations");
        if (violations.isArray()) {
            violations.forEach(violation -> {
                addSafeField(fields, violation.path("field").asText());
                addSafeField(fields, violation.path("param").asText());
            });
        }
        node.elements().forEachRemaining(child -> findFieldViolations(child, fields));
    }

    private static void append(StringBuilder result, String name, String value) {
        if (!value.isEmpty()) result.append('_').append(name).append('_').append(value);
    }

    private static void addSafeField(Set<String> fields, String candidate) {
        if (SAFE_FIELDS.contains(candidate)) fields.add(candidate);
    }

    private static String safeToken(String value) {
        String normalized = value == null ? "" : value.trim();
        return normalized.matches("[A-Za-z0-9_.-]{1,64}") ? normalized.toUpperCase(java.util.Locale.ROOT) : "";
    }
}
