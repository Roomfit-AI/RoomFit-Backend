package com.roomfit.common;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 방을 찾을 수 없습니다."),
    CONTEXT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 Agent Context를 찾을 수 없습니다."),
    LAYOUT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 배치를 찾을 수 없습니다."),
    FURNITURE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 가구를 찾을 수 없습니다."),
    STYLE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 취향 이미지를 찾을 수 없습니다."),
    PRODUCT_NOT_FOUND(HttpStatus.BAD_REQUEST, "해당 Mock Product를 찾을 수 없습니다."),
    INVALID_FURNITURE_STATUS(HttpStatus.BAD_REQUEST, "가구 status 값이 올바르지 않습니다."),
    INVALID_LIFESTYLE_GOAL(HttpStatus.BAD_REQUEST, "생활 목적 값이 올바르지 않습니다."),
    INVALID_DESIGN_STYLE(HttpStatus.BAD_REQUEST, "디자인 취향 값이 올바르지 않습니다."),
    INVALID_FURNITURE_TYPE(HttpStatus.BAD_REQUEST, "가구 타입 값이 올바르지 않습니다."),
    INVALID_ROOM_DIMENSION(HttpStatus.BAD_REQUEST, "방 크기 값이 올바르지 않습니다."),
    INVALID_OPENING_DATA(HttpStatus.BAD_REQUEST, "문/창문 데이터가 올바르지 않습니다."),
    REQUIRED_ITEM_EMPTY(HttpStatus.BAD_REQUEST, "requiredItems는 최소 1개 이상이어야 합니다."),
    STYLE_IMAGE_EMPTY(HttpStatus.BAD_REQUEST, "selectedImageIds는 최소 1개 이상이어야 합니다."),
    INVALID_FURNITURE_POSITION(HttpStatus.BAD_REQUEST, "가구 좌표가 방 범위를 벗어납니다."),
    INVALID_ROTATION(HttpStatus.BAD_REQUEST, "rotation 값이 올바르지 않습니다."),
    FURNITURE_ARRAY_MISMATCH(HttpStatus.BAD_REQUEST, "요청 furniture 배열이 기존 배치와 일치하지 않습니다."),
    UNSUPPORTED_FEEDBACK_INTENT(HttpStatus.BAD_REQUEST, "지원하지 않는 피드백입니다."),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST, "요청 본문이 올바르지 않습니다."),
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
