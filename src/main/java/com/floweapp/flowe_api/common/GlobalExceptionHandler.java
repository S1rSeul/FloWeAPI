package com.floweapp.flowe_api.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private String extractPath(WebRequest request) {
        String desc = request.getDescription(false);
        if (desc != null && desc.startsWith("uri=")) {
            return desc.substring(4);
        }
        return desc;
    }

    private ResponseEntity<ErrorResponse> buildError(HttpStatus status, String error, String message, WebRequest request) {
        ErrorResponse body = ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(extractPath(request))
                .build();
        return ResponseEntity.status(status).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception e, WebRequest request) {
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "Internal server error", e.getMessage(), request);
    }
}
