package com.takarub.esim.identity.domain.verification.exception;

import com.takarub.esim.identity.domain.verification.VerificationErrorCode;
import com.takarub.esim.identity.domain.verification.VerificationId;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when an already-consumed verification is reused.
 */
public class VerificationConsumedException extends BusinessException {

    public VerificationConsumedException(VerificationId verificationId) {
        super(VerificationErrorCode.VERIFICATION_CONSUMED,
                "Verification " + verificationId.value() + " has already been consumed.");
    }
}
