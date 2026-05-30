package com.linhdv.efms_core_service.exception;

import com.linhdv.efms_core_service.dto.common.ApiResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleNotFound(
            EntityNotFoundException ex,
            HttpServletRequest request
    ) {
        logClientError(HttpStatus.NOT_FOUND, ex, request);
        return error(HttpStatus.NOT_FOUND, ex.getMessage(), "RESOURCE_NOT_FOUND", request, null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleValidation(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = error instanceof FieldError fieldError
                    ? fieldError.getField()
                    : error.getObjectName();
            errors.put(fieldName, error.getDefaultMessage());
        });

        log.warn(
                "Validation failed: traceId={}, method={}, path={}, errors={}",
                traceId(),
                request.getMethod(),
                fullPath(request),
                errors
        );
        return error(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ", "VALIDATION_ERROR", request, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                errors.put(violation.getPropertyPath().toString(), violation.getMessage())
        );

        log.warn(
                "Constraint violation: traceId={}, method={}, path={}, errors={}",
                traceId(),
                request.getMethod(),
                fullPath(request),
                errors
        );
        return error(HttpStatus.BAD_REQUEST, "Tham số không hợp lệ", "CONSTRAINT_VIOLATION", request, errors);
    }

    @ExceptionHandler({
            IllegalArgumentException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleBadRequest(
            Exception ex,
            HttpServletRequest request
    ) {
        logClientError(HttpStatus.BAD_REQUEST, ex, request);
        return error(HttpStatus.BAD_REQUEST, resolveBadRequestMessage(ex), "BAD_REQUEST", request, null);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleInvalidState(
            IllegalStateException ex,
            HttpServletRequest request
    ) {
        logClientError(HttpStatus.CONFLICT, ex, request);
        return error(HttpStatus.CONFLICT, ex.getMessage(), "INVALID_STATE", request, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleDataIntegrity(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        logServerError(HttpStatus.CONFLICT, ex, request);
        return error(
                HttpStatus.CONFLICT,
                "Dữ liệu bị trùng hoặc vi phạm ràng buộc hệ thống",
                "DATA_INTEGRITY_VIOLATION",
                request,
                null
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAuthentication(
            AuthenticationException ex,
            HttpServletRequest request
    ) {
        logClientError(HttpStatus.UNAUTHORIZED, ex, request);
        return error(HttpStatus.UNAUTHORIZED, "Bạn cần đăng nhập để thực hiện thao tác này", "UNAUTHORIZED", request, null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleAccessDenied(
            AccessDeniedException ex,
            HttpServletRequest request
    ) {
        logClientError(HttpStatus.FORBIDDEN, ex, request);
        return error(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này", "ACCESS_DENIED", request, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        logClientError(HttpStatus.METHOD_NOT_ALLOWED, ex, request);
        return error(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method không được hỗ trợ", "METHOD_NOT_ALLOWED", request, null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Map<String, Object>>> handleGeneric(
            Exception ex,
            HttpServletRequest request
    ) {
        logServerError(HttpStatus.INTERNAL_SERVER_ERROR, ex, request);
        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Lỗi hệ thống nội bộ. Vui lòng thử lại sau.",
                "INTERNAL_SERVER_ERROR",
                request,
                null
        );
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> error(
            HttpStatus status,
            String message,
            String code,
            HttpServletRequest request,
            Object details
    ) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", code);
        data.put("traceId", traceId());
        data.put("timestamp", Instant.now().toString());
        data.put("path", request.getRequestURI());
        data.put("method", request.getMethod());
        if (details != null) {
            data.put("details", details);
        }

        return ResponseEntity
                .status(status)
                .body(ApiResponse.error(status.value(), message, data));
    }

    private void logClientError(HttpStatus status, Exception ex, HttpServletRequest request) {
        log.warn(
                "Request failed: status={}, traceId={}, method={}, path={}, userId={}, companyId={}, exception={}, message={}",
                status.value(),
                traceId(),
                request.getMethod(),
                fullPath(request),
                headerOrDash(request, "X-User-Id"),
                headerOrDash(request, "X-Company-Id"),
                ex.getClass().getSimpleName(),
                ex.getMessage()
        );
    }

    private void logServerError(HttpStatus status, Exception ex, HttpServletRequest request) {
        log.error(
                "Request failed: status={}, traceId={}, method={}, path={}, userId={}, companyId={}, exception={}, message={}",
                status.value(),
                traceId(),
                request.getMethod(),
                fullPath(request),
                headerOrDash(request, "X-User-Id"),
                headerOrDash(request, "X-Company-Id"),
                ex.getClass().getSimpleName(),
                ex.getMessage(),
                ex
        );
    }

    private String resolveBadRequestMessage(Exception ex) {
        if (ex instanceof MethodArgumentTypeMismatchException mismatch) {
            return "Tham số '" + mismatch.getName() + "' không đúng định dạng";
        }
        if (ex instanceof MissingServletRequestParameterException missing) {
            return "Thiếu tham số bắt buộc: " + missing.getParameterName();
        }
        if (ex instanceof HttpMessageNotReadableException) {
            return "Request body không hợp lệ hoặc sai định dạng JSON";
        }
        return ex.getMessage();
    }

    private String traceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : "-";
    }

    private String fullPath(HttpServletRequest request) {
        String query = request.getQueryString();
        return query == null ? request.getRequestURI() : request.getRequestURI() + "?" + query;
    }

    private String headerOrDash(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null || value.isBlank() ? "-" : value;
    }
}
