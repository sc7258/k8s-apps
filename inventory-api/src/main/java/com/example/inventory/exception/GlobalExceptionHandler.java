package com.example.inventory.exception;

import com.example.inventory.model.ErrorDetail;
import com.example.inventory.model.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFoundException(NoResourceFoundException ex) {
        return createErrorResponse(ErrorCode.ITEM_NOT_FOUND, "Resource not found: " + ex.getResourcePath(), new ArrayList<>());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> {
                    ErrorDetail detail = new ErrorDetail();
                    detail.setField(error.getField());
                    detail.setValue(String.valueOf(error.getRejectedValue()));
                    detail.setReason(error.getDefaultMessage());
                    return detail;
                })
                .collect(Collectors.toList());

        return createErrorResponse(ErrorCode.INVALID_INPUT_VALUE, null, details);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        return createErrorResponse(ErrorCode.ITEM_NOT_FOUND, ex.getMessage(), new ArrayList<>());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneralException(Exception ex) {
        log.error("Unhandled exception occurred", ex);
        return createErrorResponse(ErrorCode.INTERNAL_SERVER_ERROR, "An unexpected error occurred.", new ArrayList<>());
    }

    private ResponseEntity<ErrorResponse> createErrorResponse(ErrorCode errorCode, String customMessage, List<ErrorDetail> details) {
        ErrorResponse error = new ErrorResponse();
        error.setErrorCode(errorCode.getCode());
        error.setErrorMessage(customMessage != null ? customMessage : errorCode.getMessage());
        error.setErrorDetails(details);
        
        return ResponseEntity.status(errorCode.getStatus()).body(error);
    }
}
