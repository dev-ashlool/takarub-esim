package com.takarub.esim.identity.application.exception;

import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.shared.exception.ResourceNotFoundException;

/**
 * Thrown by application use cases when a referenced session cannot be loaded.
 */
public class SessionNotFoundApplicationException extends ResourceNotFoundException {

    public SessionNotFoundApplicationException(SessionId sessionId) {
        super(IdentityApplicationErrorCode.SESSION_NOT_FOUND,
                "Session " + sessionId.value() + " was not found.");
    }
}
