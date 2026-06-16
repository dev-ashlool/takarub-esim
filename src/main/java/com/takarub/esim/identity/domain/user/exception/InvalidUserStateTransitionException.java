package com.takarub.esim.identity.domain.user.exception;

import com.takarub.esim.identity.domain.user.UserErrorCode;
import com.takarub.esim.identity.domain.user.UserStatus;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when a user state transition is not permitted by the lifecycle rules.
 */
public class InvalidUserStateTransitionException extends BusinessException {

    public InvalidUserStateTransitionException(UserStatus from, UserStatus to) {
        super(UserErrorCode.INVALID_USER_STATE_TRANSITION,
                "Cannot transition user from " + from + " to " + to + ".");
    }
}
