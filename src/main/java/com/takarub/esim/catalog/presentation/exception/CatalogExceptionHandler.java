package com.takarub.esim.catalog.presentation.exception;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.takarub.esim.catalog.presentation.controller.CatalogController;
import com.takarub.esim.catalog.domain.exceptions.PackageNotFoundException;
import com.takarub.esim.identity.presentation.shared.ErrorResponse;

/**
 * Maps catalog-specific exceptions to HTTP responses for catalog REST endpoints.
 */
@RestControllerAdvice(assignableTypes = CatalogController.class)
public class CatalogExceptionHandler {

    private static final String PACKAGE_NOT_FOUND_CODE = "CATALOG_PACKAGE_NOT_FOUND";

    @ExceptionHandler(PackageNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePackageNotFound(PackageNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(
                        PACKAGE_NOT_FOUND_CODE,
                        ex.getMessage(),
                        Instant.now()));
    }
}
