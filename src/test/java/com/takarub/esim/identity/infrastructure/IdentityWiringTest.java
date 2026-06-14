package com.takarub.esim.identity.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

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

/**
 * Boots the full Spring context against the test (H2) datasource, exercising Flyway migration and
 * Hibernate schema validation, and asserts that the infrastructure beans and all explicitly wired
 * use-case beans are present.
 */
@SpringBootTest
class IdentityWiringTest {

    @Autowired
    private TransactionRunner transactionRunner;
    @Autowired
    private PasswordHasher passwordHasher;
    @Autowired
    private NotificationSender notificationSender;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SessionRepository sessionRepository;
    @Autowired
    private VerificationRepository verificationRepository;

    @Autowired
    private RegisterUserUseCase registerUserUseCase;
    @Autowired
    private VerifyEmailUseCase verifyEmailUseCase;
    @Autowired
    private CreateSessionUseCase createSessionUseCase;
    @Autowired
    private ConfirmPasswordResetUseCase confirmPasswordResetUseCase;
    @Autowired
    private RefreshSessionUseCase refreshSessionUseCase;
    @Autowired
    private RevokeSessionUseCase revokeSessionUseCase;
    @Autowired
    private RequestPasswordResetUseCase requestPasswordResetUseCase;
    @Autowired
    private GetUserByIdUseCase getUserByIdUseCase;
    @Autowired
    private GetUserByEmailUseCase getUserByEmailUseCase;
    @Autowired
    private GetSessionByIdUseCase getSessionByIdUseCase;

    @Test
    void infrastructurePortsAreWired() {
        assertThat(transactionRunner).isNotNull();
        assertThat(passwordHasher).isNotNull();
        assertThat(notificationSender).isNotNull();
        assertThat(userRepository).isNotNull();
        assertThat(sessionRepository).isNotNull();
        assertThat(verificationRepository).isNotNull();
    }

    @Test
    void allUseCasesAreWired() {
        assertThat(registerUserUseCase).isNotNull();
        assertThat(verifyEmailUseCase).isNotNull();
        assertThat(createSessionUseCase).isNotNull();
        assertThat(confirmPasswordResetUseCase).isNotNull();
        assertThat(refreshSessionUseCase).isNotNull();
        assertThat(revokeSessionUseCase).isNotNull();
        assertThat(requestPasswordResetUseCase).isNotNull();
        assertThat(getUserByIdUseCase).isNotNull();
        assertThat(getUserByEmailUseCase).isNotNull();
        assertThat(getSessionByIdUseCase).isNotNull();
    }
}
