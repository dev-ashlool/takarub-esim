package com.takarub.esim.identity.infrastructure.security;

import org.springframework.stereotype.Component;

import com.takarub.esim.identity.domain.session.Session;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.session.SessionRepository;
import com.takarub.esim.identity.domain.session.SessionStatus;
import com.takarub.esim.identity.domain.session.exception.SessionExpiredException;
import com.takarub.esim.identity.domain.session.exception.SessionRevokedException;
import com.takarub.esim.identity.shared.exception.UnauthorizedException;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Validates that a session referenced by an access token is still eligible for authentication.
 * Delegates session state rules to the {@link Session} aggregate without embedding business logic
 * in servlet filters.
 */
@Component
public class AuthenticatedSessionValidator {

    private final SessionRepository sessionRepository;
    private final ClockProvider clock;

    public AuthenticatedSessionValidator(SessionRepository sessionRepository, ClockProvider clock) {
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    public Session validateActiveSession(SessionId sessionId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new UnauthorizedException("Session no longer exists."));

        if (session.status() == SessionStatus.REVOKED) {
            throw new SessionRevokedException(session.id());
        }
        if (session.status() == SessionStatus.EXPIRED || session.isExpired(clock)) {
            throw new SessionExpiredException(session.id());
        }
        if (session.status() != SessionStatus.ACTIVE) {
            throw new UnauthorizedException("Session is not active.");
        }
        return session;
    }
}
