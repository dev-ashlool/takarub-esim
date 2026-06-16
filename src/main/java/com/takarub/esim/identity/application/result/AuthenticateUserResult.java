package com.takarub.esim.identity.application.result;

import java.time.Instant;

import com.takarub.esim.identity.domain.session.RefreshToken;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.user.UserId;

/**
 * Outcome of a successful authentication: issued access token, session identity, refresh token and
 * session expiry.
 */
public record AuthenticateUserResult(
        UserId userId,
        SessionId sessionId,
        String accessToken,
        RefreshToken refreshToken,
        Instant expiresAt
) {
}
