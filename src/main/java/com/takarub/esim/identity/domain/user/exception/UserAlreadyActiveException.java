package com.takarub.esim.identity.domain.user.exception;

import com.takarub.esim.identity.domain.user.UserErrorCode;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when an already-active user is asked to verify/activate again.
 */
public class UserAlreadyActiveException extends BusinessException {

    public UserAlreadyActiveException(UserId userId) {
        super(UserErrorCode.USER_ALREADY_ACTIVE, "User " + userId.value() + " is already active.");
    }
}
