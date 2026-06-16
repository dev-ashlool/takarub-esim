package com.takarub.esim.identity.domain.user.exception;

import com.takarub.esim.identity.domain.user.UserErrorCode;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when a suspended user attempts an action reserved for active users (e.g. authentication).
 */
public class UserSuspendedException extends BusinessException {

    public UserSuspendedException(UserId userId) {
        super(UserErrorCode.USER_SUSPENDED, "User " + userId.value() + " is suspended.");
    }
}
