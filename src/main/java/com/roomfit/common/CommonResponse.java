package com.roomfit.common;

public class CommonResponse<T> {

    private final boolean success;
    private final T data;
    private final ErrorPayload error;

    private CommonResponse(boolean success, T data, ErrorPayload error) {
        this.success = success;
        this.data = data;
        this.error = error;
    }

    public static <T> CommonResponse<T> ok(T data) {
        return new CommonResponse<>(true, data, null);
    }

    public static <T> CommonResponse<T> fail(ErrorCode errorCode) {
        return new CommonResponse<>(false, null,
                new ErrorPayload(errorCode.name(), errorCode.getMessage(), RequestIdContext.get()));
    }

    public boolean isSuccess() {
        return success;
    }

    public T getData() {
        return data;
    }

    public ErrorPayload getError() {
        return error;
    }

    public static class ErrorPayload {
        private final String code;
        private final String message;
        private final String requestId;

        public ErrorPayload(String code, String message) {
            this(code, message, null);
        }

        public ErrorPayload(String code, String message, String requestId) {
            this.code = code;
            this.message = message;
            this.requestId = requestId;
        }

        public String getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }

        public String getRequestId() {
            return requestId;
        }
    }
}
