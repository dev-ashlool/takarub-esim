package com.takarub.esim.identity.domain.user.exception;

import com.takarub.esim.identity.domain.user.UserErrorCode;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when a user that has not verified their email attempts an action reserved for active
 * users (e.g. authentication).
 */
public class UserNotVerifiedException extends BusinessException {

    public UserNotVerifiedException(UserId userId) {
        super(UserErrorCode.USER_NOT_VERIFIED,
                "User " + userId.value() + " must verify their email before authenticating.");
    }
}
