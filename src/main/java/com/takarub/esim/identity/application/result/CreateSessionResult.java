package com.takarub.esim.identity.application.result;

import java.time.Instant;

import com.takarub.esim.identity.domain.session.RefreshToken;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.user.UserId;

/**
 * Outcome of creating a session: the new session's identity, owning user, the refresh token to hand
 * back to the caller and the session expiry.
 */
public record CreateSessionResult(
        SessionId sessionId,
        UserId userId,
        RefreshToken refreshToken,
        Instant expiresAt
) {
}
