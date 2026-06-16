package com.takarub.esim.identity.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.identity.domain.session.DeviceMetadata;
import com.takarub.esim.identity.domain.session.RefreshToken;
import com.takarub.esim.identity.domain.session.Session;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.session.SessionStatus;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.infrastructure.persistence.entity.SessionJpaEntity;

class SessionPersistenceMapperTest {

    private final SessionPersistenceMapper mapper = new SessionPersistenceMapper();

    @Test
    void roundTripPreservesAllFields() {
        SessionId id = SessionId.of(UUID.randomUUID());
        UserId userId = UserId.of(UUID.randomUUID());
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-05T08:00:00Z");
        Instant expiresAt = Instant.parse("2026-01-02T00:00:00Z");
        Instant lastActivityAt = Instant.parse("2026-01-04T00:00:00Z");
        RefreshToken token = RefreshToken.of("opaque-token-value");
        DeviceMetadata device = DeviceMetadata.of("iPhone", "mobile", "10.0.0.1", "agent/1.0",
                lastActivityAt);

        Session session = Session.reconstitute(id, createdAt, updatedAt, userId, token,
                SessionStatus.ACTIVE, device, expiresAt);

        SessionJpaEntity entity = mapper.toEntity(session);

        assertThat(entity.getId()).isEqualTo(id.value().toString());
        assertThat(entity.getUserId()).isEqualTo(userId.value().toString());
        assertThat(entity.getRefreshToken()).isEqualTo("opaque-token-value");
        assertThat(entity.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(entity.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);
        assertThat(entity.getDeviceMetadata().getDeviceName()).isEqualTo("iPhone");
        assertThat(entity.getDeviceMetadata().getDeviceType()).isEqualTo("mobile");
        assertThat(entity.getDeviceMetadata().getIpAddress()).isEqualTo("10.0.0.1");
        assertThat(entity.getDeviceMetadata().getUserAgent()).isEqualTo("agent/1.0");
        assertThat(entity.getDeviceMetadata().getLastActivityAt()).isEqualTo(lastActivityAt);

        Session back = mapper.toDomain(entity);

        assertThat(back.id()).isEqualTo(id);
        assertThat(back.userId()).isEqualTo(userId);
        assertThat(back.refreshToken()).isEqualTo(token);
        assertThat(back.status()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(back.deviceMetadata()).isEqualTo(device);
        assertThat(back.expiresAt()).isEqualTo(expiresAt);
        assertThat(back.createdAt()).isEqualTo(createdAt);
        assertThat(back.updatedAt()).isEqualTo(updatedAt);
    }
}
