package com.firstpress.global.exception;

import com.firstpress.global.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    // 비즈니스 로직에서 의도적으로 던진 예외
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        log.warn("[Business Exception] {}: {}", e.getErrorCode().getCode(), e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), e.getMessage()));
    }

    // @Valid 검증 실패
    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException e, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        log.warn("[MethodArgumentNotValid Exception] {}", e.getMessage());

        Map<String, String> errors = e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        fe -> Objects.requireNonNullElse(fe.getDefaultMessage(), "유효하지 않은 값입니다."),
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new));

        ErrorCode errorCode = ErrorCode.INVALID_INPUT_VALUE;
        return handleExceptionInternal(e,
                ApiResponse.error(errorCode.getCode(), errorCode.getMessage(), errors),
                headers, status, request);
    }

    // 404, 405, 415 등 Spring MVC 표준 예외는 모두 여기를 거쳐 ApiResponse로 변환
    @Override
    protected @Nullable ResponseEntity<Object> handleExceptionInternal(
            Exception e, @Nullable Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (!(body instanceof ApiResponse<?>)) {
            log.warn("[{}] {}", e.getClass().getSimpleName(), e.getMessage());
            ErrorCode errorCode = resolveErrorCode(statusCode);
            body = ApiResponse.error(errorCode.getCode(), errorCode.getMessage());
        }
        // 응답 커밋 여부 확인 등 부모의 기본 처리는 그대로 유지
        return super.handleExceptionInternal(e, body, headers, statusCode, request);
    }

    // 위에서 처리되지 않은 모든 예외
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[Unhandled Exception]: ", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(errorCode.getHttpStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage()));
    }

    // HTTP 상태 코드는 Spring이 정한 값을 그대로 쓰고, code/message만 ErrorCode에서 가져옴
    private ErrorCode resolveErrorCode(HttpStatusCode statusCode) {
        return switch (statusCode.value()) {
            case 400 -> ErrorCode.INVALID_INPUT_VALUE;
            case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
            case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
            default -> statusCode.is4xxClientError() ? ErrorCode.INVALID_REQUEST : ErrorCode.INTERNAL_SERVER_ERROR;
        };
    }
}
