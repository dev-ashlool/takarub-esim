package com.takarub.esim.identity.domain.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.identity.domain.user.UserId;

class SessionReconstituteTest {

    @Test
    void restoresAllStateIncludingDistinctUpdatedAtAndTerminalStatus() {
        SessionId id = SessionId.of(UUID.randomUUID());
        UserId userId = UserId.of(UUID.randomUUID());
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-05T08:00:00Z");
        Instant expiresAt = Instant.parse("2026-01-02T00:00:00Z");
        RefreshToken token = RefreshToken.of("opaque-token-value");
        DeviceMetadata device = DeviceMetadata.of("iPhone", "mobile", "10.0.0.1", "agent/1.0",
                Instant.parse("2026-01-04T00:00:00Z"));

        Session session = Session.reconstitute(id, createdAt, updatedAt, userId, token,
                SessionStatus.REVOKED, device, expiresAt);

        assertThat(session.id()).isEqualTo(id);
        assertThat(session.userId()).isEqualTo(userId);
        assertThat(session.refreshToken()).isEqualTo(token);
        assertThat(session.status()).isEqualTo(SessionStatus.REVOKED);
        assertThat(session.deviceMetadata()).isEqualTo(device);
        assertThat(session.expiresAt()).isEqualTo(expiresAt);
        assertThat(session.createdAt()).isEqualTo(createdAt);
        assertThat(session.updatedAt()).isEqualTo(updatedAt);
    }
}
