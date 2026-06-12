package com.takarub.esim.identity.application.usecase;

import java.time.Duration;

import com.takarub.esim.identity.application.command.RequestPasswordResetCommand;
import com.takarub.esim.identity.application.exception.UserNotFoundApplicationException;
import com.takarub.esim.identity.application.port.NotificationSender;
import com.takarub.esim.identity.application.result.RequestPasswordResetResult;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.User;
import com.takarub.esim.identity.domain.user.UserRepository;
import com.takarub.esim.identity.domain.verification.Verification;
import com.takarub.esim.identity.domain.verification.VerificationRepository;
import com.takarub.esim.identity.domain.verification.VerificationType;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Requests a password reset for the account owning the supplied e-mail.
 *
 * <p>Workflow: load the user, issue a password-reset verification, persist it and notify the user
 * (the notification references the verification by VerificationId). Owns the transaction boundary
 * for the request-password-reset workflow.
 */
public class RequestPasswordResetUseCase {

    private final UserRepository userRepository;
    private final VerificationRepository verificationRepository;
    private final NotificationSender notificationSender;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;
    private final Duration passwordResetTtl;

    public RequestPasswordResetUseCase(UserRepository userRepository,
                                       VerificationRepository verificationRepository,
                                       NotificationSender notificationSender,
                                       IdGenerator idGenerator,
                                       ClockProvider clock,
                                       Duration passwordResetTtl) {
        this.userRepository = userRepository;
        this.verificationRepository = verificationRepository;
        this.notificationSender = notificationSender;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.passwordResetTtl = passwordResetTtl;
    }

    public RequestPasswordResetResult execute(RequestPasswordResetCommand command) {
        EmailAddress email = EmailAddress.of(command.email());
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundApplicationException(email));

        Verification verification = Verification.issue(idGenerator, clock, user.id(),
                VerificationType.PASSWORD_RESET, passwordResetTtl);

        verificationRepository.save(verification);
        notificationSender.sendPasswordResetInstructions(email, verification.id());

        return new RequestPasswordResetResult(user.id(), verification.id());
    }
}
