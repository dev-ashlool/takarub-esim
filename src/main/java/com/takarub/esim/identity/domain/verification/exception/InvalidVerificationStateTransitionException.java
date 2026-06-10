package com.takarub.esim.identity.domain.verification.exception;

import com.takarub.esim.identity.domain.verification.VerificationErrorCode;
import com.takarub.esim.identity.domain.verification.VerificationStatus;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when a verification state transition is not permitted by the lifecycle rules.
 */
public class InvalidVerificationStateTransitionException extends BusinessException {

    public InvalidVerificationStateTransitionException(VerificationStatus from, VerificationStatus to) {
        super(VerificationErrorCode.INVALID_VERIFICATION_STATE_TRANSITION,
                "Cannot transition verification from " + from + " to " + to + ".");
    }
}
