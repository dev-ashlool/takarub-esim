package com.takarub.esim.identity.domain.user.exception;

import com.takarub.esim.identity.domain.user.UserErrorCode;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when a locked user attempts an action reserved for active users (e.g. authentication).
 */
public class UserLockedException extends BusinessException {

    public UserLockedException(UserId userId) {
        super(UserErrorCode.USER_LOCKED, "User " + userId.value() + " is locked.");
    }
}
