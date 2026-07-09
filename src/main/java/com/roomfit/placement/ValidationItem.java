package com.roomfit.placement;

public class ValidationItem {

    private final String type;
    private final boolean passed;
    private final String message;

    public ValidationItem(String type, boolean passed, String message) {
        this.type = type;
        this.passed = passed;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getMessage() {
        return message;
    }
}
