package com.takarub.esim.identity.application.usecase;

import java.util.Optional;

import com.takarub.esim.identity.application.command.ConfirmPasswordResetCommand;
import com.takarub.esim.identity.application.exception.UserNotFoundApplicationException;
import com.takarub.esim.identity.application.exception.VerificationNotFoundApplicationException;
import com.takarub.esim.identity.application.port.PasswordHasher;
import com.takarub.esim.identity.application.result.ConfirmPasswordResetResult;
import com.takarub.esim.identity.domain.session.Session;
import com.takarub.esim.identity.domain.session.SessionRepository;
import com.takarub.esim.identity.domain.user.PasswordHash;
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
 * Confirms a password reset.
 *
 * <p>Workflow: load and consume the password-reset verification, load the user, hash and update the
 * password, then revoke the user's active session. Persists the verification, user and any revoked
 * session. Owns the transaction boundary for the confirm-password-reset workflow.
 *
 * <p>A successful reset revokes all active sessions (an approved decision). Under the approved
 * one-active-session-per-user rule there is at most one such session, so revocation is performed
 * inline here.
 */
public class ConfirmPasswordResetUseCase {

    private final VerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordHasher passwordHasher;
    private final ClockProvider clock;

    public ConfirmPasswordResetUseCase(VerificationRepository verificationRepository,
                                       UserRepository userRepository,
                                       SessionRepository sessionRepository,
                                       PasswordHasher passwordHasher,
                                       ClockProvider clock) {
        this.verificationRepository = verificationRepository;
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
    }

    public ConfirmPasswordResetResult execute(ConfirmPasswordResetCommand command) {
        VerificationId verificationId = VerificationId.of(command.verificationId());
        Verification verification = verificationRepository.findById(verificationId)
                .orElseThrow(() -> new VerificationNotFoundApplicationException(verificationId));
        if (verification.type() != VerificationType.PASSWORD_RESET) {
            throw new ValidationException("Verification is not a password reset");
        }
        verification.consume(clock);

        UserId userId = verification.userId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundApplicationException(userId));
        PasswordHash newPasswordHash = passwordHasher.hash(command.newRawPassword());
        user.changePassword(newPasswordHash, clock);

        int revokedSessionCount = 0;
        Optional<Session> activeSession = sessionRepository.findActiveSessionByUser(userId);
        if (activeSession.isPresent()) {
            Session session = activeSession.get();
            session.revoke(clock);
            sessionRepository.save(session);
            revokedSessionCount = 1;
        }

        verificationRepository.save(verification);
        userRepository.save(user);

        return new ConfirmPasswordResetResult(user.id(), revokedSessionCount);
    }
}
