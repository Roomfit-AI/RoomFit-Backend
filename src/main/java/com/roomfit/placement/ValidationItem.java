package com.roomfit.placement;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프론트 체크리스트 UI용 단일 검증 항목")
public class ValidationItem {

    @Schema(description = "검증 타입", example = "collision", allowableValues = {"collision", "boundary", "door_clearance", "window_clearance", "path"})
    private final String type;
    @Schema(description = "검증 통과 여부", example = "true")
    private final boolean passed;
    @Schema(description = "사용자에게 표시 가능한 검증 메시지", example = "가구 충돌 없음")
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
