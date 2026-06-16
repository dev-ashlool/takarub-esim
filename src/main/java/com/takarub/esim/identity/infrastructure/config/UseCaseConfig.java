package com.takarub.esim.identity.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.takarub.esim.identity.application.port.NotificationSender;
import com.takarub.esim.identity.application.port.PasswordHasher;
import com.takarub.esim.identity.application.port.TransactionRunner;
import com.takarub.esim.identity.application.usecase.ConfirmPasswordResetUseCase;
import com.takarub.esim.identity.application.usecase.CreateSessionUseCase;
import com.takarub.esim.identity.application.usecase.GetSessionByIdUseCase;
import com.takarub.esim.identity.application.usecase.GetUserByEmailUseCase;
import com.takarub.esim.identity.application.usecase.GetUserByIdUseCase;
import com.takarub.esim.identity.application.usecase.RefreshSessionUseCase;
import com.takarub.esim.identity.application.usecase.RegisterUserUseCase;
import com.takarub.esim.identity.application.usecase.RequestPasswordResetUseCase;
import com.takarub.esim.identity.application.usecase.RevokeSessionUseCase;
import com.takarub.esim.identity.application.usecase.VerifyEmailUseCase;
import com.takarub.esim.identity.domain.session.SessionRepository;
import com.takarub.esim.identity.domain.user.UserRepository;
import com.takarub.esim.identity.domain.verification.VerificationRepository;
import com.takarub.esim.identity.shared.id.IdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;

/**
 * Explicit Spring wiring for the framework-free application use cases. Use cases carry no Spring
 * annotations; they are instantiated here as beans with their collaborators injected by type. The
 * four multi-write use cases additionally receive the {@link TransactionRunner} that owns their
 * transaction boundary; single-write commands and read-only queries do not.
 */
@Configuration
@EnableConfigurationProperties(IdentityProperties.class)
public class UseCaseConfig {

    @Bean
    public RegisterUserUseCase registerUserUseCase(TransactionRunner transactionRunner,
                                                   UserRepository userRepository,
                                                   VerificationRepository verificationRepository,
                                                   PasswordHasher passwordHasher,
                                                   NotificationSender notificationSender,
                                                   IdGenerator idGenerator,
                                                   ClockProvider clockProvider,
                                                   IdentityProperties properties) {
        return new RegisterUserUseCase(transactionRunner, userRepository, verificationRepository,
                passwordHasher, notificationSender, idGenerator, clockProvider,
                properties.emailVerificationTtl());
    }

    @Bean
    public VerifyEmailUseCase verifyEmailUseCase(TransactionRunner transactionRunner,
                                                 VerificationRepository verificationRepository,
                                                 UserRepository userRepository,
                                                 ClockProvider clockProvider) {
        return new VerifyEmailUseCase(transactionRunner, verificationRepository, userRepository,
                clockProvider);
    }

    @Bean
    public CreateSessionUseCase createSessionUseCase(TransactionRunner transactionRunner,
                                                     UserRepository userRepository,
                                                     SessionRepository sessionRepository,
                                                     IdGenerator idGenerator,
                                                     ClockProvider clockProvider,
                                                     IdentityProperties properties) {
        return new CreateSessionUseCase(transactionRunner, userRepository, sessionRepository,
                idGenerator, clockProvider, properties.sessionTtl());
    }

    @Bean
    public ConfirmPasswordResetUseCase confirmPasswordResetUseCase(
            TransactionRunner transactionRunner,
            VerificationRepository verificationRepository,
            UserRepository userRepository,
            SessionRepository sessionRepository,
            PasswordHasher passwordHasher,
            ClockProvider clockProvider) {
        return new ConfirmPasswordResetUseCase(transactionRunner, verificationRepository,
                userRepository, sessionRepository, passwordHasher, clockProvider);
    }

    @Bean
    public RefreshSessionUseCase refreshSessionUseCase(SessionRepository sessionRepository,
                                                       IdGenerator idGenerator,
                                                       ClockProvider clockProvider,
                                                       IdentityProperties properties) {
        return new RefreshSessionUseCase(sessionRepository, idGenerator, clockProvider,
                properties.sessionTtl());
    }

    @Bean
    public RevokeSessionUseCase revokeSessionUseCase(SessionRepository sessionRepository,
                                                     ClockProvider clockProvider) {
        return new RevokeSessionUseCase(sessionRepository, clockProvider);
    }

    @Bean
    public RequestPasswordResetUseCase requestPasswordResetUseCase(
            UserRepository userRepository,
            VerificationRepository verificationRepository,
            NotificationSender notificationSender,
            IdGenerator idGenerator,
            ClockProvider clockProvider,
            IdentityProperties properties) {
        return new RequestPasswordResetUseCase(userRepository, verificationRepository,
                notificationSender, idGenerator, clockProvider, properties.passwordResetTtl());
    }

    @Bean
    public GetUserByIdUseCase getUserByIdUseCase(UserRepository userRepository) {
        return new GetUserByIdUseCase(userRepository);
    }

    @Bean
    public GetUserByEmailUseCase getUserByEmailUseCase(UserRepository userRepository) {
        return new GetUserByEmailUseCase(userRepository);
    }

    @Bean
    public GetSessionByIdUseCase getSessionByIdUseCase(SessionRepository sessionRepository) {
        return new GetSessionByIdUseCase(sessionRepository);
    }
}
