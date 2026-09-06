package com.takarub.esim.commerce.presentation.order.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.takarub.esim.commerce.application.exception.OrderNotFoundApplicationException;
import com.takarub.esim.commerce.presentation.order.controller.CustomerOrderController;
import com.takarub.esim.identity.presentation.shared.ErrorResponse;
import com.takarub.esim.identity.shared.exception.ErrorCode;
import com.takarub.esim.identity.shared.exception.ForbiddenException;

/**
 * Maps customer-order exceptions that would otherwise fall through GlobalExceptionHandler as
 * generic {@code BaseException} → 422.
 */
@RestControllerAdvice(assignableTypes = CustomerOrderController.class)
public class CustomerOrderExceptionHandler {

    @ExceptionHandler(OrderNotFoundApplicationException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundApplicationException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        errorCode.code(),
                        ex.getMessage() != null ? ex.getMessage() : errorCode.defaultMessage(),
                        Instant.now()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        errorCode.code(),
                        ex.getMessage() != null ? ex.getMessage() : errorCode.defaultMessage(),
                        Instant.now()));
    }
}
