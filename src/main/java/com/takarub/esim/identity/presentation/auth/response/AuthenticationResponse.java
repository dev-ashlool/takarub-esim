package com.takarub.esim.identity.presentation.auth.response;

import java.time.Instant;

public record AuthenticationResponse(
        String userId,
        String sessionId,
        String accessToken,
        String refreshToken,
        Instant expiresAt
) {
}
