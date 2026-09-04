package com.takarub.esim.commerce.presentation.dev.payment.exception;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.takarub.esim.commerce.application.exception.PaymentAttemptNotFoundApplicationException;
import com.takarub.esim.commerce.presentation.dev.payment.controller.DevPaymentNotificationController;
import com.takarub.esim.identity.presentation.shared.ErrorResponse;
import com.takarub.esim.identity.shared.exception.ErrorCode;

/**
 * Maps payment-attempt not-found to 404 for the conditional dev notification controller
 * (GlobalExceptionHandler would otherwise treat BaseException as 422).
 */
@RestControllerAdvice(assignableTypes = DevPaymentNotificationController.class)
@ConditionalOnProperty(
        prefix = "takarub.commerce.dev-payment-verification",
        name = "enabled",
        havingValue = "true")
public class DevPaymentNotificationExceptionHandler {

    @ExceptionHandler(PaymentAttemptNotFoundApplicationException.class)
    public ResponseEntity<ErrorResponse> handlePaymentAttemptNotFound(
            PaymentAttemptNotFoundApplicationException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        errorCode.code(),
                        ex.getMessage() != null ? ex.getMessage() : errorCode.defaultMessage(),
                        Instant.now()));
    }
}
