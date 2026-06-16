package com.takarub.esim.identity.application.exception;

import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.exception.ResourceNotFoundException;

/**
 * Thrown by application use cases when a referenced user cannot be loaded. Distinct from domain
 * exceptions; carries an application error code for unified error translation.
 */
public class UserNotFoundApplicationException extends ResourceNotFoundException {

    public UserNotFoundApplicationException(UserId userId) {
        super(IdentityApplicationErrorCode.USER_NOT_FOUND,
                "User " + userId.value() + " was not found.");
    }

    public UserNotFoundApplicationException(EmailAddress email) {
        super(IdentityApplicationErrorCode.USER_NOT_FOUND,
                "User with e-mail " + email.value() + " was not found.");
    }
}
