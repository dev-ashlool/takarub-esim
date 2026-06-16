package com.takarub.esim.identity.application.exception;

import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.shared.exception.ConflictException;

/**
 * Thrown during registration when the supplied e-mail is already associated with an existing user.
 */
public class DuplicateUserApplicationException extends ConflictException {

    public DuplicateUserApplicationException(EmailAddress email) {
        super(IdentityApplicationErrorCode.DUPLICATE_USER,
                "A user with e-mail " + email.value() + " already exists.");
    }
}
