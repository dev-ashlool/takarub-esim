package com.takarub.esim.identity.domain.session.exception;

import com.takarub.esim.identity.domain.session.SessionErrorCode;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when an expired session is used.
 */
public class SessionExpiredException extends BusinessException {

    public SessionExpiredException(SessionId sessionId) {
        super(SessionErrorCode.SESSION_EXPIRED, "Session " + sessionId.value() + " has expired.");
    }
}
