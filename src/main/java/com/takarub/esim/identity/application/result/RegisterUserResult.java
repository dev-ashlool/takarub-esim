package com.takarub.esim.identity.application.result;

import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserStatus;
import com.takarub.esim.identity.domain.verification.VerificationId;

/**
 * Outcome of registering a user: the new user's identity and status plus the issued e-mail
 * verification's identifier. The opaque token is delivered out-of-band and is not returned here.
 */
public record RegisterUserResult(
        UserId userId,
        EmailAddress email,
        UserStatus status,
        VerificationId verificationId
) {
}
