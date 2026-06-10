package com.takarub.esim.identity.domain.session.exception;

import com.takarub.esim.identity.domain.session.SessionErrorCode;
import com.takarub.esim.identity.domain.session.SessionStatus;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when a session state transition is not permitted by the lifecycle rules.
 */
public class InvalidSessionStateTransitionException extends BusinessException {

    public InvalidSessionStateTransitionException(SessionStatus from, SessionStatus to) {
        super(SessionErrorCode.INVALID_SESSION_STATE_TRANSITION,
                "Cannot transition session from " + from + " to " + to + ".");
    }
}
