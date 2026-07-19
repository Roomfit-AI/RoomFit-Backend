package com.roomfit.placement;

import com.fasterxml.jackson.databind.JsonNode;
import com.roomfit.common.CustomException;
import com.roomfit.common.ErrorCode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

public class FeedbackPlanValidator {

    static final int MAX_FEEDBACK_OPERATIONS = 4;
    private static final Set<FeedbackRelation> MOVE_RELATIONS = Set.of(
            FeedbackRelation.LEFT, FeedbackRelation.RIGHT, FeedbackRelation.FORWARD,
            FeedbackRelation.BACKWARD, FeedbackRelation.NEAR_WINDOW, FeedbackRelation.NEAR_WALL,
            FeedbackRelation.AWAY_FROM_DOOR, FeedbackRelation.CENTER
    );
    private static final Set<FeedbackRelation> ADD_RELATIONS = Set.of(
            FeedbackRelation.NEXT_TO, FeedbackRelation.LEFT_OF, FeedbackRelation.RIGHT_OF,
            FeedbackRelation.NEAR_WALL, FeedbackRelation.NEAR_WINDOW,
            FeedbackRelation.IN_CORNER, FeedbackRelation.CENTER
    );
    private static final Set<String> FORBIDDEN_PROVIDER_FIELDS = Set.of(
            "x", "z", "coordinates", "position", "distancemeters", "rotation", "rotationdegrees",
            "angle", "angledegrees", "score", "totalscore", "scoresummary", "validation", "validationresult",
            "weight", "objectiveweight", "productid", "variantid"
    );

    public void validateProviderResponse(JsonNode root) {
        rejectForbiddenFields(root);
    }

    public void validate(FeedbackPlan plan) {
        validate(plan, MAX_FEEDBACK_OPERATIONS);
    }

    public void validate(FeedbackPlan plan, int maxOperations) {
        require(plan != null && "2.0".equals(plan.version()));
        require(plan.requestKind() != null);
        require(maxOperations > 0 && plan.operations().size() <= maxOperations);
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
        validateTarget(operation.target());
        if (operation.referenceTarget() != null) {
            require(!operation.referenceTarget().isEmpty());
            validateTarget(operation.referenceTarget());
        }
        switch (operation.type()) {
            case MOVE -> {
                require(operation.placement() != null);
                require(MOVE_RELATIONS.contains(operation.placement().relation()));
                require(operation.placement().magnitude() != null);
                require(operation.placement().orientation() == null);
                require(operation.placement().side() == null);
                require(operation.constraints() == null);
                require(operation.referenceTarget() == null);
                require(operation.productRequirements() == null);
                require(operation.replacementRequirements() == null);
            }
            case ROTATE -> {
                require(operation.placement() != null);
                require(operation.placement().orientation() != null);
                require(operation.placement().relation() == null);
                require(operation.placement().magnitude() == null);
                require(operation.placement().side() == null);
                require(operation.constraints() == null);
                require(operation.referenceTarget() == null);
                require(operation.productRequirements() == null);
                require(operation.replacementRequirements() == null);
            }
            case REPLACE_PRODUCT -> {
                require(operation.placement() == null);
                require(hasSelectionConstraint(operation.constraints()));
                require(operation.referenceTarget() == null);
                require(operation.productRequirements() == null);
                require(operation.replacementRequirements() == null);
                String targetType = operation.target().furnitureType();
                String constraintType = operation.constraints().furnitureType();
                require(targetType.isBlank() || constraintType.isBlank() || targetType.equals(constraintType));
            }
            case ADD_FURNITURE -> {
                require(operation.constraints() == null);
                require(operation.replacementRequirements() == null);
                require(operation.productRequirements() != null);
                validateProductRequirements(operation.productRequirements());
                require(operation.target().furnitureId().isBlank());
                require(!operation.target().furnitureType().isBlank());
                require(operation.target().furnitureType().equals(operation.productRequirements().furnitureType()));
                require(operation.placement() != null && ADD_RELATIONS.contains(operation.placement().relation()));
                require(operation.placement().magnitude() == null && operation.placement().orientation() == null);
                boolean requiresReference = operation.placement().relation() == FeedbackRelation.NEXT_TO
                        || operation.placement().relation() == FeedbackRelation.LEFT_OF
                        || operation.placement().relation() == FeedbackRelation.RIGHT_OF;
                require(!requiresReference || operation.referenceTarget() != null);
                require(operation.placement().relation() == FeedbackRelation.NEXT_TO
                        || operation.placement().side() == null);
            }
            case REMOVE_FURNITURE -> {
                require(operation.referenceTarget() == null);
                require(operation.placement() == null);
                require(operation.constraints() == null);
                require(operation.productRequirements() == null);
                require(operation.replacementRequirements() == null);
            }
            case SWAP_FURNITURE -> {
                require(operation.referenceTarget() == null);
                require(operation.placement() == null);
                require(operation.constraints() == null);
                require(operation.productRequirements() == null);
                require(operation.replacementRequirements() != null);
                validateProductRequirements(operation.replacementRequirements());
            }
            case CHANGE_MATERIAL, CHANGE_COLOR_TONE -> invalid();
        }
    }

    private void validateTarget(FeedbackTargetSelector target) {
        require(target.ordinal() == null || target.ordinal() > 0);
        require(target.locationHint() == null || target.furnitureId().isBlank());
        require(target.ordinal() == null || target.furnitureId().isBlank());
        require(target.locationHint() == null || target.ordinal() == null);
        require((target.locationHint() == null && target.ordinal() == null)
                || !target.furnitureType().isBlank());
    }

    private void validateProductRequirements(FeedbackProductRequirements requirements) {
        require(!requirements.furnitureType().isBlank());
        require(requirements.sizePreference() != null);
        Set<String> keywords = new HashSet<>();
        for (String keyword : requirements.styleKeywords()) {
            require(keyword != null && !keyword.isBlank());
            require(keywords.add(keyword));
        }
    }

    private boolean hasSelectionConstraint(FeedbackReplaceConstraints constraints) {
        return constraints != null
                && constraints.furnitureType() != null
                && !constraints.furnitureType().isBlank()
                && (constraints.largerThanCurrent()
                || constraints.smallerThanCurrent()
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
                String normalizedFieldName = fieldName.toLowerCase(Locale.ROOT)
                        .replace("_", "")
                        .replace("-", "");
                require(!FORBIDDEN_PROVIDER_FIELDS.contains(normalizedFieldName));
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
