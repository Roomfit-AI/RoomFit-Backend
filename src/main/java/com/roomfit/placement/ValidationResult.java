package com.roomfit.placement;

import java.util.List;

public class ValidationResult {

    private final boolean collisionFree;
    private final boolean doorClearance;
    private final boolean windowClearance;
    private final boolean pathSecured;
    private final List<String> warnings;

    public ValidationResult(boolean collisionFree, boolean doorClearance, boolean windowClearance,
                             boolean pathSecured, List<String> warnings) {
        this.collisionFree = collisionFree;
        this.doorClearance = doorClearance;
        this.windowClearance = windowClearance;
        this.pathSecured = pathSecured;
        this.warnings = warnings;
    }

    public boolean isCollisionFree() {
        return collisionFree;
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

    public List<String> getWarnings() {
        return warnings;
    }
}
