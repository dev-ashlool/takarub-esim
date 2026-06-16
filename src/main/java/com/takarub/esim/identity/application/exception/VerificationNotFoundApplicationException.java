package com.takarub.esim.identity.application.exception;

import com.takarub.esim.identity.domain.verification.VerificationId;
import com.takarub.esim.identity.shared.exception.ResourceNotFoundException;

/**
 * Thrown by application use cases when a referenced verification cannot be loaded.
 */
public class VerificationNotFoundApplicationException extends ResourceNotFoundException {

    public VerificationNotFoundApplicationException(VerificationId verificationId) {
        super(IdentityApplicationErrorCode.VERIFICATION_NOT_FOUND,
                "Verification " + verificationId.value() + " was not found.");
    }
}
