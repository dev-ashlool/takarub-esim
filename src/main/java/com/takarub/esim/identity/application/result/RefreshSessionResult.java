package com.takarub.esim.identity.application.result;

import java.time.Instant;

import com.takarub.esim.identity.domain.session.RefreshToken;
import com.takarub.esim.identity.domain.session.SessionId;

/**
 * Outcome of refreshing a session: the session identity, the rotated refresh token and the new
 * expiry. Exposes the refresh information the caller needs to continue the session.
 */
public record RefreshSessionResult(
        SessionId sessionId,
        RefreshToken refreshToken,
        Instant expiresAt
) {
}
