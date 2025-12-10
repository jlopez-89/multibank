package com.multibank.candle.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        log.error("Runtime error:", ex);
        var runtimeError = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(runtimeError)
                .body(new ErrorResponse(runtimeError.value(), ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        log.error("Unexpected error:", ex);
        var internalServerError = HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(internalServerError)
                .body(new ErrorResponse(internalServerError.value(), "Unexpected server error"));
    }

    public record ErrorResponse(int code, String message) {
    }
}