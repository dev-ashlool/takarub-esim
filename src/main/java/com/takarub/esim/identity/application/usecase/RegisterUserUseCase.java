package com.takarub.esim.identity.application.usecase;

import java.time.Duration;

import com.takarub.esim.identity.application.command.RegisterUserCommand;
import com.takarub.esim.identity.application.exception.DuplicateUserApplicationException;
import com.takarub.esim.identity.application.port.NotificationSender;
import com.takarub.esim.identity.application.port.PasswordHasher;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.application.result.RegisterUserResult;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.PasswordHash;
import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.User;
import com.takarub.esim.identity.domain.user.UserRepository;
import com.takarub.esim.identity.domain.verification.Verification;
import com.takarub.esim.identity.domain.verification.VerificationRepository;
import com.takarub.esim.identity.domain.verification.VerificationType;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Registers a new user.
 *
 * <p>Workflow: check e-mail uniqueness, hash the password, create the user aggregate, issue an
 * e-mail verification, persist both aggregates and notify the user (the notification references the
 * verification by VerificationId). Owns the transaction boundary for the registration workflow
 * (transaction wiring arrives in a later task).
 */
public class RegisterUserUseCase {

    /** Self-registration always creates a customer; admin provisioning is a separate concern. */
    private static final Role REGISTRATION_ROLE = Role.CUSTOMER;

    private final TransactionRunner transactionRunner;
    private final UserRepository userRepository;
    private final VerificationRepository verificationRepository;
    private final PasswordHasher passwordHasher;
    private final NotificationSender notificationSender;
    private final IdGenerator idGenerator;
    private final ClockProvider clock;
    private final Duration emailVerificationTtl;

    public RegisterUserUseCase(TransactionRunner transactionRunner,
                               UserRepository userRepository,
                               VerificationRepository verificationRepository,
                               PasswordHasher passwordHasher,
                               NotificationSender notificationSender,
                               IdGenerator idGenerator,
                               ClockProvider clock,
                               Duration emailVerificationTtl) {
        this.transactionRunner = transactionRunner;
        this.userRepository = userRepository;
        this.verificationRepository = verificationRepository;
        this.passwordHasher = passwordHasher;
        this.notificationSender = notificationSender;
        this.idGenerator = idGenerator;
        this.clock = clock;
        this.emailVerificationTtl = emailVerificationTtl;
    }

    public RegisterUserResult execute(RegisterUserCommand command) {
        return transactionRunner.execute(() -> {
            EmailAddress email = EmailAddress.of(command.email());
            if (userRepository.existsByEmail(email)) {
                throw new DuplicateUserApplicationException(email);
            }

            PasswordHash passwordHash = passwordHasher.hash(command.rawPassword());
            User user = User.register(idGenerator, clock, email, passwordHash, REGISTRATION_ROLE);
            Verification verification = Verification.issue(idGenerator, clock, user.id(),
                    VerificationType.EMAIL_VERIFICATION, emailVerificationTtl);

            userRepository.save(user);
            verificationRepository.save(verification);
            notificationSender.sendEmailVerification(email, verification.id());

            return new RegisterUserResult(user.id(), email, user.status(), verification.id());
        });
    }
}
