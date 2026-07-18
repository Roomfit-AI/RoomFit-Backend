package com.roomfit.placement;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class FeedbackPlanValidator {

    private static final int MAX_OPERATIONS = 4;
    private static final Set<String> FORBIDDEN_PROVIDER_FIELDS = Set.of(
            "x", "z", "coordinates", "position", "distancemeters", "rotation", "rotationdegrees",
            "angle", "angledegrees", "score", "totalscore", "scoresummary", "validation", "validationresult",
            "weight", "objectiveweight", "productid", "variantid"
    );

    public void validateProviderResponse(JsonNode root) {
        rejectForbiddenFields(root);
    }

    public void validate(FeedbackPlan plan) {
        require(plan != null && "2.0".equals(plan.version()));
        require(plan.requestKind() != null);
        require(plan.operations().size() <= MAX_OPERATIONS);
        require(plan.goals().isEmpty());

        switch (plan.requestKind()) {
            case DIRECT -> require(plan.operations().size() == 1 && plan.clarification() == null);
            case COMPOSITE -> require(plan.operations().size() >= 2 && plan.clarification() == null);
            case CLARIFICATION -> require(plan.operations().isEmpty()
                    && plan.clarification() != null
                    && !plan.clarification().question().isBlank());
            case ABSTRACT -> invalid();
        }

        Set<String> precedingOperationIds = new HashSet<>();
        for (FeedbackOperation operation : plan.operations()) {
            require(operation != null && !operation.operationId().isBlank());
            require(precedingOperationIds.add(operation.operationId()));
            for (String dependency : operation.dependsOn()) {
                require(dependency != null && !dependency.isBlank());
                require(!operation.operationId().equals(dependency));
                require(precedingOperationIds.contains(dependency));
            }
            validateOperation(operation);
        }
    }

    private void validateOperation(FeedbackOperation operation) {
        require(operation.type() != null);
        require(operation.target() != null && !operation.target().isEmpty());
        switch (operation.type()) {
            case MOVE -> {
                require(operation.placement() != null);
                require(operation.placement().relation() != null);
                require(operation.placement().magnitude() != null);
                require(operation.placement().orientation() == null);
                require(operation.constraints() == null);
            }
            case ROTATE -> {
                require(operation.placement() != null);
                require(operation.placement().orientation() != null);
                require(operation.placement().relation() == null);
                require(operation.placement().magnitude() == null);
                require(operation.constraints() == null);
            }
            case REPLACE_PRODUCT -> {
                require(operation.placement() == null);
                require(hasSelectionConstraint(operation.constraints()));
                String targetType = operation.target().furnitureType();
                String constraintType = operation.constraints().furnitureType();
                require(targetType.isBlank() || constraintType.isBlank() || targetType.equals(constraintType));
            }
            case ADD_FURNITURE, REMOVE_FURNITURE, SWAP_FURNITURE, CHANGE_MATERIAL, CHANGE_COLOR_TONE -> invalid();
        }
    }

    private boolean hasSelectionConstraint(FeedbackReplaceConstraints constraints) {
        return constraints != null
                && constraints.furnitureType() != null
                && !constraints.furnitureType().isBlank()
                && (constraints.largerThanCurrent()
                || constraints.minWidth() != null
                || !constraints.requiredStyleTags().isEmpty()
                || !constraints.requiredLifestyleTags().isEmpty()
                || constraints.storagePreferred());
    }

    private void rejectForbiddenFields(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            Iterator<String> names = node.fieldNames();
            while (names.hasNext()) {
                String fieldName = names.next();
                require(!FORBIDDEN_PROVIDER_FIELDS.contains(fieldName.toLowerCase(Locale.ROOT)));
                rejectForbiddenFields(node.get(fieldName));
            }
            return;
        }
        if (node.isArray()) {
            node.forEach(this::rejectForbiddenFields);
        }
    }

    private void require(boolean condition) {
        if (!condition) {
            invalid();
        }
    }

    private void invalid() {
        throw new CustomException(ErrorCode.INVALID_REQUEST_BODY);
    }
}
