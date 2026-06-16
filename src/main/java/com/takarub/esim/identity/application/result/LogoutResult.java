package com.takarub.esim.identity.application.result;

import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.session.SessionStatus;

/**
 * Outcome of logging out: the revoked session identity and its resulting status.
 */
public record LogoutResult(SessionId sessionId, SessionStatus status) {
}
