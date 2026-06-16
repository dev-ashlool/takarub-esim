package com.takarub.esim.identity.domain.verification;

import java.util.Optional;

import com.takarub.esim.identity.domain.user.UserId;

/**
 * Repository port for the Verification aggregate. Implementations live in the infrastructure layer
 * (not in this task).
 */
public interface VerificationRepository {

    Verification save(Verification verification);

    Optional<Verification> findById(VerificationId verificationId);

    Optional<Verification> findActiveVerification(UserId userId, VerificationType type);
}
