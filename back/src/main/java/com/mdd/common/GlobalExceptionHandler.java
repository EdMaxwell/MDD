package com.mdd.common;

import com.mdd.auth.exception.EmailAlreadyUsedException;
import com.mdd.auth.exception.InvalidCredentialsException;
import com.mdd.auth.exception.InvalidRefreshTokenException;
import com.mdd.auth.exception.SuspiciousRefreshTokenReuseException;
import com.mdd.post.exception.PostNotFoundException;
import com.mdd.topic.exception.TopicNotFoundException;
import com.mdd.user.exception.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Converts application and validation exceptions into stable API error responses.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final Clock clock;

    public GlobalExceptionHandler(Clock clock) {
        this.clock = clock;
    }

    /**
     * Converts Jakarta Validation field errors into a field-to-message map.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        Map<String, String> details = new LinkedHashMap<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            details.put(error.getField(), error.getDefaultMessage());
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "Validation failed", details);
    }

    /**
     * Returns a conflict when the requested email is already bound to another account.
     */
    @ExceptionHandler(EmailAlreadyUsedException.class)
    public ResponseEntity<ApiErrorResponse> handleEmailAlreadyUsed(EmailAlreadyUsedException exception) {
        return buildResponse(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    /**
     * Returns a generic unauthorized response for bad credentials.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidCredentials(InvalidCredentialsException exception) {
        return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), null);
    }

    /**
     * Returns unauthorized when the refresh token cannot be used.
     */
    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidRefreshToken(InvalidRefreshTokenException exception) {
        return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), null);
    }

    /**
     * Returns unauthorized after suspicious refresh-token reuse has been detected.
     */
    @ExceptionHandler(SuspiciousRefreshTokenReuseException.class)
    public ResponseEntity<ApiErrorResponse> handleSuspiciousRefreshTokenReuse(
            SuspiciousRefreshTokenReuseException exception
    ) {
        return buildResponse(HttpStatus.UNAUTHORIZED, exception.getMessage(), null);
    }

    /**
     * Returns forbidden when an authenticated user is not allowed to access a resource.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied", null);
    }

    /**
     * Returns not found for missing users.
     */
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleUserNotFound(UserNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    /**
     * Returns not found for missing topics.
     */
    @ExceptionHandler(TopicNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTopicNotFound(TopicNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    /**
     * Returns not found for missing posts.
     */
    @ExceptionHandler(PostNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handlePostNotFound(PostNotFoundException exception) {
        return buildResponse(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    /**
     * Last-resort handler that prevents internal exception details from leaking to clients.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception exception, HttpServletRequest request) {
        LOGGER.error("Unhandled exception on {} {}", request.getMethod(), request.getRequestURI(), exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error on " + request.getRequestURI(), null);
    }

    /**
     * Builds the shared API error envelope.
     */
    private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String message, Map<String, String> details) {
        ApiErrorResponse payload = new ApiErrorResponse(
                Instant.now(clock),
                status.value(),
                status.getReasonPhrase(),
                message,
                details
        );
        return ResponseEntity.status(status).body(payload);
    }
}
