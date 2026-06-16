package com.takarub.esim.identity.application.usecase;

import com.takarub.esim.identity.application.command.RevokeSessionCommand;
import com.takarub.esim.identity.application.exception.SessionNotFoundApplicationException;
import com.takarub.esim.identity.application.result.RevokeSessionResult;
import com.takarub.esim.identity.domain.session.Session;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.session.SessionRepository;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Revokes a session.
 *
 * <p>Workflow: load the session, revoke it and persist. Owns the transaction boundary for the
 * revoke-session workflow.
 */
public class RevokeSessionUseCase {

    private final SessionRepository sessionRepository;
    private final ClockProvider clock;

    public RevokeSessionUseCase(SessionRepository sessionRepository, ClockProvider clock) {
        this.sessionRepository = sessionRepository;
        this.clock = clock;
    }

    public RevokeSessionResult execute(RevokeSessionCommand command) {
        SessionId sessionId = SessionId.of(command.sessionId());
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundApplicationException(sessionId));

        session.revoke(clock);
        sessionRepository.save(session);

        return new RevokeSessionResult(session.id(), session.status());
    }
}
