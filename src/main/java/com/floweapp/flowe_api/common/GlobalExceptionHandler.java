package com.floweapp.flowe_api.common;

import com.floweapp.flowe_api.auth.exception.EmailAlreadyExistsException;
import com.floweapp.flowe_api.auth.exception.InvalidCredentialsException;
import com.floweapp.flowe_api.auth.exception.InvalidTokenException;
import com.floweapp.flowe_api.couple.exception.CoupleAlreadyExistsException;
import com.floweapp.flowe_api.couple.exception.CoupleNotFoundException;
import com.floweapp.flowe_api.couple.exception.InviteCodeGenerationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleNotValid(
            MethodArgumentNotValidException e,
            WebRequest request
    ) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fieldError ->
                        fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("; "));

        return buildError(
                HttpStatus.BAD_REQUEST,
                "Validation Failed",
                message,
                request
        );
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyExists(
            EmailAlreadyExistsException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                "Conflict",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ErrorResponse> handleInvalidToken(
            InvalidTokenException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.UNAUTHORIZED,
                "Unauthorized",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(CoupleAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleCoupleAlreadyExists(
            CoupleAlreadyExistsException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                "Conflict",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(CoupleNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCoupleNotFound(
            CoupleNotFoundException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "Not Found",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(InviteCodeGenerationException.class)
    public ResponseEntity<ErrorResponse> handleInviteCodeGeneration(
            InviteCodeGenerationException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "Not Found",
                e.getMessage(),
                request
        );
    }
}
