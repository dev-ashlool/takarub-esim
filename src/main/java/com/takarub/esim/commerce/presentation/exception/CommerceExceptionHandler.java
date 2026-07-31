package com.takarub.esim.commerce.presentation.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.takarub.esim.commerce.application.exception.CartNotFoundApplicationException;
import com.takarub.esim.commerce.presentation.cart.controller.CartController;
import com.takarub.esim.identity.presentation.shared.ErrorResponse;
import com.takarub.esim.identity.shared.exception.ErrorCode;

/**
 * Maps commerce-specific exceptions to HTTP responses for cart REST endpoints.
 */
@RestControllerAdvice(assignableTypes = CartController.class)
public class CommerceExceptionHandler {

    @ExceptionHandler(CartNotFoundApplicationException.class)
    public ResponseEntity<ErrorResponse> handleCartNotFound(CartNotFoundApplicationException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        errorCode.code(),
                        ex.getMessage() != null ? ex.getMessage() : errorCode.defaultMessage(),
                        Instant.now()));
    }
}
