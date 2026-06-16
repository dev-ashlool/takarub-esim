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

import com.takarub.esim.identity.domain.session.DeviceMetadata;
import com.takarub.esim.identity.domain.session.Session;
import com.takarub.esim.identity.domain.session.SessionStatus;
import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserStatus;
import com.takarub.esim.identity.infrastructure.persistence.entity.UserJpaEntity;
import com.takarub.esim.identity.infrastructure.persistence.mapper.SessionPersistenceMapper;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.identity.shared.time.SystemClockProvider;

/**
 * Integration test (@DataJpaTest, H2 in MySQL mode, Flyway-built schema) for the Session repository
 * adapter. Named {@code *Test} so it runs in the Surefire test phase.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({SessionRepositoryAdapter.class, SessionPersistenceMapper.class})
class SessionRepositoryAdapterTest {

    private static final Instant FIXED = Instant.parse("2026-06-13T10:15:30.123456Z");
    private static final Duration TTL = Duration.ofHours(24);

    @Autowired
    private SessionRepositoryAdapter adapter;

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

    private Session newSession(UserId userId) {
        DeviceMetadata device = DeviceMetadata.of("iPhone", "mobile", "10.0.0.1", "agent/1.0", FIXED);
        return Session.start(idGenerator, clock, userId, device, TTL);
    }

    @Test
    void savesAndReloadsByIdWithAllFields() {
        UserId userId = persistOwner();
        Session session = newSession(userId);
        adapter.save(session);
        entityManager.flush();
        entityManager.clear();

        Optional<Session> found = adapter.findById(session.id());

        assertThat(found).isPresent();
        assertThat(found.get().userId()).isEqualTo(userId);
        assertThat(found.get().refreshToken()).isEqualTo(session.refreshToken());
        assertThat(found.get().status()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(found.get().expiresAt()).isEqualTo(FIXED.plus(TTL));
        assertThat(found.get().deviceMetadata().deviceName()).isEqualTo("iPhone");
        assertThat(found.get().createdAt()).isEqualTo(FIXED);
    }

    @Test
    void findsActiveSessionByUserAndExcludesRevoked() {
        UserId userId = persistOwner();
        Session session = newSession(userId);
        adapter.save(session);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findActiveSessionByUser(userId)).isPresent();

        Session toRevoke = adapter.findById(session.id()).orElseThrow();
        toRevoke.revoke(clock);
        adapter.save(toRevoke);
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findActiveSessionByUser(userId)).isEmpty();
    }
}
