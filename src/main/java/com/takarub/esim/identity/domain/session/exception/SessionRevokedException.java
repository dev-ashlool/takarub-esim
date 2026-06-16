package com.takarub.esim.identity.domain.session.exception;

import com.takarub.esim.identity.domain.session.SessionErrorCode;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when a revoked session is used.
 */
public class SessionRevokedException extends BusinessException {

    public SessionRevokedException(SessionId sessionId) {
        super(SessionErrorCode.SESSION_REVOKED, "Session " + sessionId.value() + " has been revoked.");
    }
}
