package com.takarub.esim.identity.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.verification.Verification;
import com.takarub.esim.identity.domain.verification.VerificationId;
import com.takarub.esim.identity.domain.verification.VerificationStatus;
import com.takarub.esim.identity.domain.verification.VerificationType;
import com.takarub.esim.identity.infrastructure.persistence.entity.VerificationJpaEntity;

class VerificationPersistenceMapperTest {

    private final VerificationPersistenceMapper mapper = new VerificationPersistenceMapper();

    @Test
    void roundTripPreservesAllFields() {
        VerificationId id = VerificationId.of(UUID.randomUUID());
        UserId userId = UserId.of(UUID.randomUUID());
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-01T00:10:00Z");
        Instant expiresAt = Instant.parse("2026-01-01T01:00:00Z");

        Verification verification = Verification.reconstitute(id, createdAt, updatedAt, userId,
                VerificationType.EMAIL_VERIFICATION, expiresAt, VerificationStatus.PENDING);

        VerificationJpaEntity entity = mapper.toEntity(verification);

        assertThat(entity.getId()).isEqualTo(id.value().toString());
        assertThat(entity.getUserId()).isEqualTo(userId.value().toString());
        assertThat(entity.getType()).isEqualTo(VerificationType.EMAIL_VERIFICATION);
        assertThat(entity.getStatus()).isEqualTo(VerificationStatus.PENDING);
        assertThat(entity.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);

        Verification back = mapper.toDomain(entity);

        assertThat(back.id()).isEqualTo(id);
        assertThat(back.userId()).isEqualTo(userId);
        assertThat(back.type()).isEqualTo(VerificationType.EMAIL_VERIFICATION);
        assertThat(back.status()).isEqualTo(VerificationStatus.PENDING);
        assertThat(back.expiresAt()).isEqualTo(expiresAt);
        assertThat(back.createdAt()).isEqualTo(createdAt);
        assertThat(back.updatedAt()).isEqualTo(updatedAt);
    }
}
