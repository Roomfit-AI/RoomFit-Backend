# Invalid Request Body Handling Design

## 목표

잘못된 요청 body 또는 path variable 타입 오류가 발생해도 Spring 기본 에러 body가 노출되지 않고, 프로젝트 공통 응답 구조인 `CommonResponse<T>`로 실패 응답을 반환한다.

## 범위

- 포함: `INVALID_REQUEST_BODY / 400` 공통 처리
- 포함: malformed JSON body
- 포함: path variable 타입 변환 실패
- 제외: Bean Validation 애너테이션 도입, DTO별 필수값 검증, Swagger 문서화

## 응답

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVALID_REQUEST_BODY",
    "message": "요청 본문이 올바르지 않습니다."
  }
}
```

## 구현

- `ErrorCode.INVALID_REQUEST_BODY`를 추가한다.
- `GlobalExceptionHandler`에서 `HttpMessageNotReadableException`을 처리한다.
- `GlobalExceptionHandler`에서 `MethodArgumentTypeMismatchException`을 처리한다.
- 두 예외 모두 `400 Bad Request`와 `CommonResponse.fail(ErrorCode.INVALID_REQUEST_BODY)`를 반환한다.

## 테스트

- malformed JSON body 요청은 `INVALID_REQUEST_BODY / 400`을 반환한다.
- 숫자 path variable 자리에 문자열이 들어오면 `INVALID_REQUEST_BODY / 400`을 반환한다.
