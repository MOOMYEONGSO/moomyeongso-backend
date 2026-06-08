package org.example.moomyeongso.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.moomyeongso.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.context.support.DefaultMessageSourceResolvable;

import java.util.Objects;
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Object>> handleCustomException(CustomException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        log.warn("{}: {} - {}",
                ex.getClass().getSimpleName(),
                errorCode.getCode(),
                errorCode.getMessage());
        return ApiResponse.error(
                errorCode.getStatus(),
                errorCode.getCode(),
                errorCode.getMessage()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleException(Exception ex) {
        log.error("Unhandled exception: {} - {}", ex.getClass().getSimpleName(), ex.getMessage());
        return ApiResponse.error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_SERVER_ERROR.getCode(),
                ErrorCode.INTERNAL_SERVER_ERROR.getMessage()
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(ErrorCode.INVALID_INPUT.getMessage());

        log.warn("Validation failed: {}", errorMessage);

        return ApiResponse.error(
                ErrorCode.INVALID_INPUT.getStatus(),
                ErrorCode.INVALID_INPUT.getCode(),
                errorMessage
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("Unsupported HTTP method: {}", ex.getMethod());
        return ApiResponse.error(
                ErrorCode.METHOD_NOT_ALLOWED.getStatus(),
                ErrorCode.METHOD_NOT_ALLOWED.getCode(),
                ErrorCode.METHOD_NOT_ALLOWED.getMessage()
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
        return ApiResponse.error(
                ErrorCode.INVALID_JSON.getStatus(),
                ErrorCode.INVALID_JSON.getCode(),
                ErrorCode.INVALID_JSON.getMessage()
        );
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ApiResponse<Object>> handleMissingServletRequestPart(MissingServletRequestPartException ex) {
        log.warn("Missing multipart request part: {}", ex.getRequestPartName());
        return ApiResponse.error(
                ErrorCode.INVALID_INPUT.getStatus(),
                ErrorCode.INVALID_INPUT.getCode(),
                ErrorCode.INVALID_INPUT.getMessage()
        );
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Object>> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex) {
        log.warn("Multipart upload too large: {}", ex.getMessage());
        return ApiResponse.error(
                ErrorCode.IMAGE_TOO_LARGE.getStatus(),
                ErrorCode.IMAGE_TOO_LARGE.getCode(),
                ErrorCode.IMAGE_TOO_LARGE.getMessage()
        );
    }

    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Object>> handleMultipartException(MultipartException ex) {
        log.warn("Invalid multipart request: {}", ex.getMessage());
        return ApiResponse.error(
                ErrorCode.INVALID_INPUT.getStatus(),
                ErrorCode.INVALID_INPUT.getCode(),
                ErrorCode.INVALID_INPUT.getMessage()
        );
    }

    @ExceptionHandler({ AccessDeniedException.class, AuthorizationDeniedException.class })
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(Exception ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ApiResponse.error(
                ErrorCode.ACCESS_DENIED.getStatus(),
                ErrorCode.ACCESS_DENIED.getCode(),
                ErrorCode.ACCESS_DENIED.getMessage()
        );
    }
}
