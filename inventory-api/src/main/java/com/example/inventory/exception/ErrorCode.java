package com.example.inventory.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // Common
    INTERNAL_SERVER_ERROR("S001", "Internal Server Error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_INPUT_VALUE("C001", "Invalid Input Value", HttpStatus.BAD_REQUEST),

    // Item
    ITEM_NOT_FOUND("I001", "Item Not Found", HttpStatus.NOT_FOUND),
    ITEM_ALREADY_EXISTS("I002", "Item Already Exists", HttpStatus.CONFLICT);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
