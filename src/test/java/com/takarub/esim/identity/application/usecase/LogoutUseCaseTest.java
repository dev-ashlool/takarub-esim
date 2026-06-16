package com.takarub.esim.identity.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.takarub.esim.identity.application.command.LogoutCommand;
import com.takarub.esim.identity.application.exception.SessionNotFoundApplicationException;
import com.takarub.esim.identity.application.result.LogoutResult;
import com.takarub.esim.identity.domain.session.DeviceMetadata;
import com.takarub.esim.identity.domain.session.RefreshToken;
import com.takarub.esim.identity.domain.session.Session;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.session.SessionRepository;
import com.takarub.esim.identity.domain.session.SessionStatus;
import com.takarub.esim.identity.domain.session.exception.InvalidSessionStateTransitionException;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.shared.time.ClockProvider;

@ExtendWith(MockitoExtension.class)
class LogoutUseCaseTest {

    private static final Instant NOW = Instant.parse("2026-06-16T12:00:00Z");

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ClockProvider clock;

    private LogoutUseCase logoutUseCase;

    @BeforeEach
    void setUp() {
        logoutUseCase = new LogoutUseCase(new RevokeSessionUseCase(sessionRepository, clock));
    }

    @Test
    void revokesActiveSession() {
        when(clock.now()).thenReturn(NOW);
        SessionId sessionId = SessionId.of(UUID.randomUUID());
        Session session = activeSession(sessionId);
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        LogoutResult result = logoutUseCase.execute(new LogoutCommand(sessionId.value().toString()));

        assertThat(result.sessionId()).isEqualTo(sessionId);
        assertThat(result.status()).isEqualTo(SessionStatus.REVOKED);
        ArgumentCaptor<Session> saved = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository).save(saved.capture());
        assertThat(saved.getValue().status()).isEqualTo(SessionStatus.REVOKED);
    }

    @Test
    void rejectsAlreadyRevokedSession() {
        SessionId sessionId = SessionId.of(UUID.randomUUID());
        Session session = Session.reconstitute(
                sessionId,
                NOW,
                NOW,
                UserId.of(UUID.randomUUID()),
                RefreshToken.of("refresh-token"),
                SessionStatus.REVOKED,
                DeviceMetadata.of("iPhone", "mobile", "10.0.0.1", "agent/1.0", NOW),
                NOW.plusSeconds(3600));
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> logoutUseCase.execute(new LogoutCommand(sessionId.value().toString())))
                .isInstanceOf(InvalidSessionStateTransitionException.class);
        verify(sessionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void rejectsMissingSession() {
        SessionId sessionId = SessionId.of(UUID.randomUUID());
        when(sessionRepository.findById(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> logoutUseCase.execute(new LogoutCommand(sessionId.value().toString())))
                .isInstanceOf(SessionNotFoundApplicationException.class);
    }

    private static Session activeSession(SessionId sessionId) {
        return Session.reconstitute(
                sessionId,
                NOW,
                NOW,
                UserId.of(UUID.randomUUID()),
                RefreshToken.of("refresh-token"),
                SessionStatus.ACTIVE,
                DeviceMetadata.of("iPhone", "mobile", "10.0.0.1", "agent/1.0", NOW),
                NOW.plusSeconds(3600));
    }
}
