package com.takarub.esim.identity.application.usecase;

import com.takarub.esim.identity.application.command.VerifyEmailCommand;
import com.takarub.esim.identity.application.exception.UserNotFoundApplicationException;
import com.takarub.esim.identity.application.exception.VerificationNotFoundApplicationException;
import com.takarub.esim.identity.application.result.VerifyEmailResult;
import com.takarub.esim.identity.domain.user.User;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserRepository;
import com.takarub.esim.identity.domain.verification.Verification;
import com.takarub.esim.identity.domain.verification.VerificationId;
import com.takarub.esim.identity.domain.verification.VerificationRepository;
import com.takarub.esim.identity.domain.verification.VerificationType;
import com.takarub.esim.identity.shared.exception.ValidationException;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Verifies a user's e-mail.
 *
 * <p>Workflow: load and consume the e-mail verification, then activate the owning user. Persists
 * both aggregates. Owns the transaction boundary for the verify-email workflow.
 */
public class VerifyEmailUseCase {

    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final ClockProvider clock;

    public VerifyEmailUseCase(VerificationRepository verificationRepository,
                              UserRepository userRepository,
                              ClockProvider clock) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    public VerifyEmailResult execute(VerifyEmailCommand command) {
        VerificationId verificationId = VerificationId.of(command.verificationId());
        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new VerificationNotFoundApplicationException(verificationId));
        if (verification.type() != VerificationType.EMAIL_VERIFICATION) {
            throw new ValidationException("Verification is not an e-mail verification");
        }
        verification.consume(clock);

        UserId userId = verification.userId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundApplicationException(userId));
        user.verifyEmail(clock);

        verificationRepository.save(verification);
        userRepository.save(user);

        return new VerifyEmailResult(user.id(), user.status());
    }
}
