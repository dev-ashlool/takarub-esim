package com.takarub.esim.identity.presentation.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.takarub.esim.identity.application.exception.UserNotFoundApplicationException;
import com.takarub.esim.identity.shared.exception.BaseException;
import com.takarub.esim.identity.shared.exception.ConflictException;
import com.takarub.esim.identity.shared.exception.ErrorCode;
import com.takarub.esim.identity.shared.exception.SharedErrorCode;
import com.takarub.esim.identity.shared.exception.UnauthorizedException;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.presentation.shared.ErrorResponse;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex);
    }

    @ExceptionHandler(UserNotFoundApplicationException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFound(UserNotFoundApplicationException ex) {
        return build(HttpStatus.NOT_FOUND, ex);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflict(ConflictException ex) {
        return build(HttpStatus.CONFLICT, ex);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return build(HttpStatus.BAD_REQUEST, ex);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse(SharedErrorCode.VALIDATION_FAILED.defaultMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        SharedErrorCode.VALIDATION_FAILED.code(),
                        message,
                        Instant.now()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        SharedErrorCode.VALIDATION_FAILED.code(),
                        ex.getMessage() != null ? ex.getMessage() : SharedErrorCode.VALIDATION_FAILED.defaultMessage(),
                        Instant.now()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        SharedErrorCode.FORBIDDEN.code(),
                        ex.getMessage() != null ? ex.getMessage() : SharedErrorCode.FORBIDDEN.defaultMessage(),
                        Instant.now()));
    }

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        SharedErrorCode.INTERNAL_ERROR.code(),
                        SharedErrorCode.INTERNAL_ERROR.defaultMessage(),
                        Instant.now()));
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, BaseException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(status)
                .body(new ErrorResponse(
                        errorCode.code(),
                        ex.getMessage() != null ? ex.getMessage() : errorCode.defaultMessage(),
                        Instant.now()));
    }
}
