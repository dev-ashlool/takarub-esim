package com.takarub.esim.identity.domain.verification.exception;

import com.takarub.esim.identity.domain.verification.VerificationErrorCode;
import com.takarub.esim.identity.domain.verification.VerificationId;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when a cancelled verification is reused.
 */
public class VerificationCancelledException extends BusinessException {

    public VerificationCancelledException(VerificationId verificationId) {
        super(VerificationErrorCode.VERIFICATION_CANCELLED,
                "Verification " + verificationId.value() + " has been cancelled.");
    }
}
