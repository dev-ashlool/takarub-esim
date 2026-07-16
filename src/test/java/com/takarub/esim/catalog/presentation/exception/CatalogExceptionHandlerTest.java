package com.takarub.esim.catalog.presentation.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.takarub.esim.catalog.domain.exceptions.PackageNotFoundException;
import com.takarub.esim.identity.presentation.shared.ErrorResponse;

class CatalogExceptionHandlerTest {

    private final CatalogExceptionHandler handler = new CatalogExceptionHandler();

    @Test
    void mapsPackageNotFoundToNotFoundResponse() {
        var response = handler.handlePackageNotFound(
                new PackageNotFoundException("Catalog package not found: missing"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ErrorResponse body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.code()).isEqualTo("CATALOG_PACKAGE_NOT_FOUND");
        assertThat(body.message()).isEqualTo("Catalog package not found: missing");
        assertThat(body.timestamp()).isNotNull();
    }
}
