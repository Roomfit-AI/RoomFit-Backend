package com.roomfit.placement;

import java.util.List;

public class ValidationResult {

    private final boolean collisionFree;
    private final boolean boundaryValid;
    private final boolean doorClearance;
    private final boolean windowClearance;
    private final boolean pathSecured;
    private final List<ValidationItem> validationItems;
    private final List<String> warnings;

    public ValidationResult(boolean collisionFree, boolean boundaryValid, boolean doorClearance,
                             boolean windowClearance, boolean pathSecured,
                             List<ValidationItem> validationItems, List<String> warnings) {
        this.collisionFree = collisionFree;
        this.boundaryValid = boundaryValid;
        this.doorClearance = doorClearance;
        this.windowClearance = windowClearance;
        this.pathSecured = pathSecured;
        this.validationItems = List.copyOf(validationItems);
        this.warnings = warnings;
    }

    public boolean isCollisionFree() {
        return collisionFree;
    }

    public boolean isBoundaryValid() {
        return boundaryValid;
    }

    public boolean isDoorClearance() {
        return doorClearance;
    }

    public boolean isWindowClearance() {
        return windowClearance;
    }

    public boolean isPathSecured() {
        return pathSecured;
    }

    public List<ValidationItem> getValidationItems() {
        return validationItems;
    }

    public List<String> getWarnings() {
        return warnings;
    }
}
