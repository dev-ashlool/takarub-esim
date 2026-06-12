package com.takarub.esim.identity.application.port;

import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.verification.VerificationId;

/**
 * Application port for delivering identity notifications. Notifications reference the issued
 * verification by its {@link VerificationId}; link construction is an infrastructure concern.
 */
public interface NotificationSender {

    void sendEmailVerification(
            EmailAddress recipient,
            VerificationId verificationId);

    void sendPasswordResetInstructions(
            EmailAddress recipient,
            VerificationId verificationId);
}
