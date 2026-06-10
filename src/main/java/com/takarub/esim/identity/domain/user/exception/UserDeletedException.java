package com.takarub.esim.identity.domain.user.exception;

import com.takarub.esim.identity.domain.user.UserErrorCode;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when a deleted user attempts an action reserved for active users (e.g. authentication).
 */
public class UserDeletedException extends BusinessException {

    public UserDeletedException(UserId userId) {
        super(UserErrorCode.USER_DELETED, "User " + userId.value() + " is deleted.");
    }
}
