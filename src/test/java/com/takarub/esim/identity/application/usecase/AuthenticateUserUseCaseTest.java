package com.takarub.esim.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.identity.application.command.AuthenticateUserCommand;
import com.takarub.esim.identity.application.command.AuthenticateUserDeviceMetadata;
import com.takarub.esim.identity.application.command.CreateSessionCommand;
import com.takarub.esim.identity.application.exception.UserNotFoundApplicationException;
import com.takarub.esim.identity.application.port.AccessTokenIssuer;
import com.takarub.esim.identity.application.port.PasswordHasher;
import com.takarub.esim.identity.application.result.AuthenticateUserResult;
import com.takarub.esim.identity.application.result.CreateSessionResult;
import com.takarub.esim.identity.domain.session.RefreshToken;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.PasswordHash;
import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.User;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserRepository;
import com.takarub.esim.identity.domain.user.UserStatus;
import com.takarub.esim.identity.domain.user.exception.UserDeletedException;
import com.takarub.esim.identity.domain.user.exception.UserLockedException;
import com.takarub.esim.identity.domain.user.exception.UserNotVerifiedException;
import com.takarub.esim.identity.domain.user.exception.UserSuspendedException;
import com.takarub.esim.identity.shared.exception.UnauthorizedException;

@ExtendWith(MockitoExtension.class)
class AuthenticateUserUseCaseTest {

    private static final String EMAIL = "user@example.com";
    private static final String PASSWORD = "s3cret-password";
    private static final AuthenticateUserDeviceMetadata DEVICE = new AuthenticateUserDeviceMetadata(
            "iPhone", "mobile", "10.0.0.1", "agent/1.0");

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordHasher passwordHasher;
    @Mock
    private CreateSessionUseCase createSessionUseCase;
    @Mock
    private AccessTokenIssuer accessTokenIssuer;

    private AuthenticateUserUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new AuthenticateUserUseCase(userRepository, passwordHasher, createSessionUseCase,
                accessTokenIssuer);
    }

    @Test
    void authenticatesWithValidCredentials() {
        UserId userId = UserId.of(UUID.randomUUID());
        User user = activeUser(userId);
        SessionId sessionId = SessionId.of(UUID.randomUUID());
        RefreshToken refreshToken = RefreshToken.of("refresh-token");
        Instant expiresAt = Instant.parse("2026-06-16T12:00:00Z");

        when(userRepository.findByEmail(EmailAddress.of(EMAIL))).thenReturn(Optional.of(user));
        when(passwordHasher.matches(PASSWORD, user.passwordHash())).thenReturn(true);
        when(createSessionUseCase.execute(any(CreateSessionCommand.class)))
                .thenReturn(new CreateSessionResult(sessionId, userId, refreshToken, expiresAt));
        when(accessTokenIssuer.issueAccessToken(eq(userId), eq(sessionId), eq(user.roles()),
                eq(user.email()))).thenReturn("access-token");

        AuthenticateUserResult result = useCase.execute(
                new AuthenticateUserCommand(EMAIL, PASSWORD, DEVICE));

        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.sessionId()).isEqualTo(sessionId);
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo(refreshToken);
        assertThat(result.expiresAt()).isEqualTo(expiresAt);
        verify(createSessionUseCase).execute(any(CreateSessionCommand.class));
    }

    @Test
    void rejectsInvalidPassword() {
        User user = activeUser(UserId.of(UUID.randomUUID()));
        when(userRepository.findByEmail(EmailAddress.of(EMAIL))).thenReturn(Optional.of(user));
        when(passwordHasher.matches(PASSWORD, user.passwordHash())).thenReturn(false);

        assertThatThrownBy(() -> useCase.execute(new AuthenticateUserCommand(EMAIL, PASSWORD, DEVICE)))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void rejectsUnknownUser() {
        when(userRepository.findByEmail(EmailAddress.of(EMAIL))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(new AuthenticateUserCommand(EMAIL, PASSWORD, DEVICE)))
                .isInstanceOf(UserNotFoundApplicationException.class);
    }

    @Test
    void rejectsInactiveUserPendingVerification() {
        User user = userWithStatus(UserStatus.PENDING_VERIFICATION);
        when(userRepository.findByEmail(EmailAddress.of(EMAIL))).thenReturn(Optional.of(user));
        when(passwordHasher.matches(PASSWORD, user.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new AuthenticateUserCommand(EMAIL, PASSWORD, DEVICE)))
                .isInstanceOf(UserNotVerifiedException.class);
    }

    @Test
    void rejectsLockedUser() {
        User user = userWithStatus(UserStatus.LOCKED);
        when(userRepository.findByEmail(EmailAddress.of(EMAIL))).thenReturn(Optional.of(user));
        when(passwordHasher.matches(PASSWORD, user.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new AuthenticateUserCommand(EMAIL, PASSWORD, DEVICE)))
                .isInstanceOf(UserLockedException.class);
    }

    @Test
    void rejectsSuspendedUser() {
        User user = userWithStatus(UserStatus.SUSPENDED);
        when(userRepository.findByEmail(EmailAddress.of(EMAIL))).thenReturn(Optional.of(user));
        when(passwordHasher.matches(PASSWORD, user.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new AuthenticateUserCommand(EMAIL, PASSWORD, DEVICE)))
                .isInstanceOf(UserSuspendedException.class);
    }

    @Test
    void rejectsDeletedUser() {
        User user = userWithStatus(UserStatus.DELETED);
        when(userRepository.findByEmail(EmailAddress.of(EMAIL))).thenReturn(Optional.of(user));
        when(passwordHasher.matches(PASSWORD, user.passwordHash())).thenReturn(true);

        assertThatThrownBy(() -> useCase.execute(new AuthenticateUserCommand(EMAIL, PASSWORD, DEVICE)))
                .isInstanceOf(UserDeletedException.class);
    }

    private static User activeUser(UserId userId) {
        return User.reconstitute(userId,
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                EmailAddress.of(EMAIL),
                PasswordHash.of("$2a$10$hash"),
                UserStatus.ACTIVE,
                EnumSet.of(Role.CUSTOMER));
    }

    private static User userWithStatus(UserStatus status) {
        return User.reconstitute(UserId.of(UUID.randomUUID()),
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T00:00:00Z"),
                EmailAddress.of(EMAIL),
                PasswordHash.of("$2a$10$hash"),
                status,
                Set.of(Role.CUSTOMER));
    }
}
