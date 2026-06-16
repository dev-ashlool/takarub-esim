package com.takarub.esim.identity.presentation.auth.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank String sessionId,
        @NotBlank String refreshToken
) {
}
