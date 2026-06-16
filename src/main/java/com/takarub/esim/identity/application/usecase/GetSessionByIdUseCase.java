package com.takarub.esim.identity.application.usecase;

import com.takarub.esim.identity.application.exception.SessionNotFoundApplicationException;
import com.takarub.esim.identity.application.query.GetSessionByIdQuery;
import com.takarub.esim.identity.application.result.SessionView;
import com.takarub.esim.identity.domain.session.Session;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.session.SessionRepository;

/**
 * Read-only query use case: loads a session by identifier and projects it to a {@link SessionView}.
 */
public class GetSessionByIdUseCase {

    private final SessionRepository sessionRepository;

    public GetSessionByIdUseCase(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public SessionView execute(GetSessionByIdQuery query) {
        SessionId sessionId = SessionId.of(query.sessionId());
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new SessionNotFoundApplicationException(sessionId));
        return SessionView.from(session);
    }
}
