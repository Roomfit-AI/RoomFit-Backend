package com.roomfit.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 방을 찾을 수 없습니다."),
    CONTEXT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 Agent Context를 찾을 수 없습니다."),
    LAYOUT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 배치를 찾을 수 없습니다."),
    INVALID_FURNITURE_STATUS(HttpStatus.BAD_REQUEST, "가구 status 값이 올바르지 않습니다."),
    REQUIRED_ITEM_EMPTY(HttpStatus.BAD_REQUEST, "requiredItems는 최소 1개 이상이어야 합니다."),
    INVALID_FURNITURE_POSITION(HttpStatus.BAD_REQUEST, "가구 좌표가 방 범위를 벗어납니다."),
    ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확정된 배치입니다."),
    RECOMMENDATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "배치 추천에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }
}
