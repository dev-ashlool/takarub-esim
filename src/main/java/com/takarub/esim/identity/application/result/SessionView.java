package com.takarub.esim.identity.application.result;

import java.time.Instant;

import com.takarub.esim.identity.domain.session.Session;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.session.SessionStatus;
import com.takarub.esim.identity.domain.user.UserId;

/**
 * Immutable read model projecting a {@link Session} for query responses. The refresh token is
 * intentionally never exposed through this view.
 */
public record SessionView(
        SessionId id,
        UserId userId,
        SessionStatus status,
        Instant expiresAt,
        Instant createdAt,
        Instant updatedAt
) {

    public static SessionView from(Session session) {
        return new SessionView(
                session.id(),
                session.userId(),
                session.status(),
                session.expiresAt(),
                session.createdAt(),
                session.updatedAt());
    }
}
