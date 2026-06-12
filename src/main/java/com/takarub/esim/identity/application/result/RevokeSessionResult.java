package com.takarub.esim.identity.application.result;

import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.session.SessionStatus;

/**
 * Outcome of revoking a session: the session identity and its resulting status.
 */
public record RevokeSessionResult(SessionId sessionId, SessionStatus status) {
}
