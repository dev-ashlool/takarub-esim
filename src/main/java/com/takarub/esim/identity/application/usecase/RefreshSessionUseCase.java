package com.takarub.esim.identity.application.usecase;

import java.time.Duration;

import com.takarub.esim.identity.application.command.RefreshSessionCommand;
import com.takarub.esim.identity.application.exception.SessionNotFoundApplicationException;
import com.takarub.esim.identity.application.result.RefreshSessionResult;
import com.takarub.esim.identity.domain.session.RefreshToken;
import com.takarub.esim.identity.domain.session.Session;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.session.SessionRepository;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Refreshes (rotates) a session's refresh token.
 *
 * <p>Workflow: load the session, validate the presented refresh token, rotate it and persist.
 * Owns the transaction boundary for the refresh-session workflow.
 */
public class RefreshSessionUseCase {

    private final SessionRepository sessionRepository;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;
    private final Duration sessionTtl;

    public RefreshSessionUseCase(SessionRepository sessionRepository,
                                 IdGenerator idGenerator,
                                 ClockProvider clock,
                                 Duration sessionTtl) {
        this.sessionRepository = sessionRepository;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.sessionTtl = sessionTtl;
    }

    public RefreshSessionResult execute(RefreshSessionCommand command) {
        SessionId sessionId = SessionId.of(command.sessionId());
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundApplicationException(sessionId));

        session.verifyRefreshToken(RefreshToken.of(command.refreshToken()));
        RefreshToken rotated = session.rotateRefreshToken(idGenerator, clock, sessionTtl);
        sessionRepository.save(session);

        return new RefreshSessionResult(session.id(), rotated, session.expiresAt());
    }
}
