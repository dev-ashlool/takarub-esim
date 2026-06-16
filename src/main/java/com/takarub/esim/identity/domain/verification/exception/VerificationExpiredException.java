package com.takarub.esim.identity.domain.verification.exception;

import com.takarub.esim.identity.domain.verification.VerificationErrorCode;
import com.takarub.esim.identity.domain.verification.VerificationId;
import com.takarub.esim.identity.shared.exception.BusinessException;

/**
 * Thrown when an expired verification is used.
 */
public class VerificationExpiredException extends BusinessException {

    public VerificationExpiredException(VerificationId verificationId) {
        super(VerificationErrorCode.VERIFICATION_EXPIRED,
                "Verification " + verificationId.value() + " has expired.");
    }
}
