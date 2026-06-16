package com.takarub.esim.identity.domain.verification;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.identity.domain.user.UserId;

class VerificationReconstituteTest {

    @Test
    void restoresAllStateIncludingDistinctUpdatedAtAndTerminalStatus() {
        VerificationId id = VerificationId.of(UUID.randomUUID());
        UserId userId = UserId.of(UUID.randomUUID());
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-01-01T00:10:00Z");
        Instant expiresAt = Instant.parse("2026-01-01T01:00:00Z");

        Verification verification = Verification.reconstitute(id, createdAt, updatedAt, userId,
                VerificationType.PASSWORD_RESET, expiresAt, VerificationStatus.CONSUMED);

        assertThat(verification.id()).isEqualTo(id);
        assertThat(verification.userId()).isEqualTo(userId);
        assertThat(verification.type()).isEqualTo(VerificationType.PASSWORD_RESET);
        assertThat(verification.status()).isEqualTo(VerificationStatus.CONSUMED);
        assertThat(verification.expiresAt()).isEqualTo(expiresAt);
        assertThat(verification.createdAt()).isEqualTo(createdAt);
        assertThat(verification.updatedAt()).isEqualTo(updatedAt);
    }
}
