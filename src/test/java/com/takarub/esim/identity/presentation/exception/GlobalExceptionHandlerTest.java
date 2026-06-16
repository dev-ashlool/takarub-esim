package com.takarub.esim.identity.presentation.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import com.takarub.esim.identity.application.exception.UserNotFoundApplicationException;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.presentation.shared.ErrorResponse;
import com.takarub.esim.identity.shared.exception.SharedErrorCode;
import com.takarub.esim.identity.shared.exception.UnauthorizedException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsUnauthorizedExceptionTo401() {
        ResponseEntity<ErrorResponse> response = handler.handleUnauthorized(
                new UnauthorizedException("Invalid credentials"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().code()).isEqualTo(SharedErrorCode.UNAUTHORIZED.code());
    }

    @Test
    void mapsUserNotFoundTo404() {
        ResponseEntity<ErrorResponse> response = handler.handleUserNotFound(
                new UserNotFoundApplicationException(EmailAddress.of("missing@example.com")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().code()).isEqualTo("IDENTITY_USER_NOT_FOUND");
    }

    @Test
    void mapsAccessDeniedTo403() {
        ResponseEntity<ErrorResponse> response = handler.handleAccessDenied(
                new AccessDeniedException("Denied"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().code()).isEqualTo(SharedErrorCode.FORBIDDEN.code());
    }

    @Test
    void mapsUnexpectedExceptionTo500() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo(SharedErrorCode.INTERNAL_ERROR.code());
    }
}
