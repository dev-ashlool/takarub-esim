package com.takarub.esim.identity.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserStatus;
import com.takarub.esim.identity.domain.verification.Verification;
import com.takarub.esim.identity.domain.verification.VerificationStatus;
import com.takarub.esim.identity.domain.verification.VerificationType;
import com.takarub.esim.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.takarub.esim.identity.infrastructure.persistence.mapper.VerificationPersistenceMapper;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.identity.shared.time.SystemClockProvider;

/**
 * Integration test (@DataJpaTest, H2 in MySQL mode, Flyway-built schema) for the Verification
 * repository adapter. Named {@code *Test} so it runs in the Surefire test phase.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({VerificationRepositoryAdapter.class, VerificationPersistenceMapper.class})
class VerificationRepositoryAdapterTest {

    private static final Instant FIXED = Instant.parse("2026-06-13T10:15:30.123456Z");
    private static final Duration TTL = Duration.ofHours(1);

    @Autowired
    private VerificationRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private final ClockProvider clock = new SystemClockProvider(Clock.fixed(FIXED, ZoneOffset.UTC));

    private UserId persistOwner() {
        String userId = UUID.randomUUID().toString();
        entityManager.persist(new UserJpaEntity(userId, "owner-" + userId + "@example.com",
                "$2a$10$hash", UserStatus.ACTIVE, EnumSet.of(Role.CUSTOMER), FIXED, FIXED));
        entityManager.flush();
        return UserId.of(userId);
    }

    @Test
    void savesAndReloadsByIdWithAllFields() {
        UserId userId = persistOwner();
        Verification verification = Verification.issue(idGenerator, clock, userId,
                VerificationType.EMAIL_VERIFICATION, TTL);
        adapter.save(verification);
        entityManager.flush();
        entityManager.clear();

        Optional<Verification> found = adapter.findById(verification.id());

        assertThat(found).isPresent();
        assertThat(found.get().userId()).isEqualTo(userId);
        assertThat(found.get().type()).isEqualTo(VerificationType.EMAIL_VERIFICATION);
        assertThat(found.get().status()).isEqualTo(VerificationStatus.PENDING);
        assertThat(found.get().expiresAt()).isEqualTo(FIXED.plus(TTL));
        assertThat(found.get().createdAt()).isEqualTo(FIXED);
    }

    @Test
    void findsActiveVerificationByUserAndTypeAndExcludesConsumedOrOtherType() {
        UserId userId = persistOwner();
        Verification verification = Verification.issue(idGenerator, clock, userId,
                VerificationType.EMAIL_VERIFICATION, TTL);
        adapter.save(verification);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findActiveVerification(userId, VerificationType.EMAIL_VERIFICATION))
                .isPresent();
        assertThat(adapter.findActiveVerification(userId, VerificationType.PASSWORD_RESET))
                .isEmpty();

        Verification toConsume = adapter.findById(verification.id()).orElseThrow();
        toConsume.consume(clock);
        adapter.save(toConsume);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findActiveVerification(userId, VerificationType.EMAIL_VERIFICATION))
                .isEmpty();
    }
}
