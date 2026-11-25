package com.floweapp.flowe_api.common;

import com.floweapp.flowe_api.auth.exception.EmailAlreadyExistsException;
import com.floweapp.flowe_api.auth.exception.InvalidTokenException;
import com.floweapp.flowe_api.calendar.exception.EventNotFoundException;
import com.floweapp.flowe_api.couple.exception.*;
import com.floweapp.flowe_api.finance.exception.*;
import com.floweapp.flowe_api.task.exception.InvalidQueryParameterException;
import com.floweapp.flowe_api.task.exception.InvalidStatusTransitionException;
import com.floweapp.flowe_api.task.exception.TaskNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
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
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
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
                HttpStatus.CONFLICT.getReasonPhrase(),
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
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
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
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
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
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Неверный email или пароль",
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

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedMediaType(
            HttpMediaTypeNotSupportedException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported Media Type",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(InviteCodeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleInviteCodeNotFound(
            InviteCodeNotFoundException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "Not Found",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(CoupleAlreadyJoinedException.class)
    public ResponseEntity<ErrorResponse> handleCoupleAlreadyJoined(
            CoupleAlreadyJoinedException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                "Conflict",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(CannotJoinOwnCoupleException.class)
    public ResponseEntity<ErrorResponse> handleCannotJoinOwnCouple(
            CannotJoinOwnCoupleException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                "Conflict",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidQueryParameterException.class)
    public ResponseEntity<ErrorResponse> handleInvalidQueryParameter(
            InvalidQueryParameterException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleTaskNotFound(
            TaskNotFoundException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "Not Found",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(CoupleNotActiveException.class)
    public ResponseEntity<ErrorResponse> handleCoupleNotActive(
            CoupleNotActiveException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                "Conflict",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidStatusTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStatusTransition(
            InvalidStatusTransitionException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.CONFLICT,
                "Conflict",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Искаженный JSON-файл или недопустимое значение",
                request
        );
    }

    @ExceptionHandler(EventNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEventNotFound(
            EventNotFoundException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "Not Found",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Database constraint violation: " + e.getMostSpecificCause().getMessage(),
                request
        );
    }

    @ExceptionHandler(ExpenseNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleExpenseNotFound(
            ExpenseNotFoundException e,
            WebRequest request
    ) {
        return buildError(
                HttpStatus.NOT_FOUND,
                "Not Found",
                e.getMessage(),
                request
        );
    }

    @ExceptionHandler({BudgetNotFoundException.class, SavingsAccountNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleBudgetNotFound(RuntimeException e, WebRequest request) {
        return buildError(HttpStatus.NOT_FOUND, "Not Found", e.getMessage(), request);
    }

    @ExceptionHandler(BudgetAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleBudgetAlreadyExists(BudgetAlreadyExistsException e, WebRequest request) {
        return buildError(HttpStatus.CONFLICT, "Conflict", e.getMessage(), request);
    }

    @ExceptionHandler(InvalidBudgetException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBudget(InvalidBudgetException e, WebRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Bad Request", e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e, WebRequest request) {
        return buildError(HttpStatus.BAD_REQUEST, "Bad Request",
                "Недопустимое значение параметра: " + e.getName(), request);
    }
}
