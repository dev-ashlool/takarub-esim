package com.takarub.esim.identity.infrastructure.persistence.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.PasswordHash;
import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.User;
import com.takarub.esim.identity.domain.user.UserStatus;
import com.takarub.esim.identity.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.takarub.esim.identity.shared.id.UuidIdGenerator;
import com.takarub.esim.identity.shared.time.ClockProvider;
import com.takarub.esim.identity.shared.time.SystemClockProvider;

/**
 * Integration test (@DataJpaTest, H2 in MySQL mode, Flyway-built schema) for the User repository
 * adapter. Named {@code *Test} so it runs in the Surefire test phase.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UserRepositoryAdapter.class, UserPersistenceMapper.class})
class UserRepositoryAdapterTest {

    private static final Instant FIXED = Instant.parse("2026-06-13T10:15:30.123456Z");

    @Autowired
    private UserRepositoryAdapter adapter;

    @Autowired
    private TestEntityManager entityManager;

    private final UuidIdGenerator idGenerator = new UuidIdGenerator();
    private final ClockProvider clock = new SystemClockProvider(Clock.fixed(FIXED, ZoneOffset.UTC));

    private User newUser(String email) {
        return User.register(idGenerator, clock, EmailAddress.of(email),
                PasswordHash.of("$2a$10$hash"), Role.CUSTOMER);
    }

    @Test
    void savesAndReloadsByIdWithAllFields() {
        User user = newUser("byid@example.com");
        adapter.save(user);
        entityManager.flush();
        entityManager.clear();

        Optional<User> found = adapter.findById(user.id());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(user.id());
        assertThat(found.get().email().value()).isEqualTo("byid@example.com");
        assertThat(found.get().status()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(found.get().roles()).containsExactly(Role.CUSTOMER);
        assertThat(found.get().createdAt()).isEqualTo(FIXED);
        assertThat(found.get().updatedAt()).isEqualTo(FIXED);
    }

    @Test
    void findsByEmailAndChecksExistence() {
        adapter.save(newUser("byemail@example.com"));
        entityManager.flush();
        entityManager.clear();

        assertThat(adapter.findByEmail(EmailAddress.of("byemail@example.com"))).isPresent();
        assertThat(adapter.existsByEmail(EmailAddress.of("byemail@example.com"))).isTrue();
        assertThat(adapter.existsByEmail(EmailAddress.of("missing@example.com"))).isFalse();
    }

    @Test
    void enforcesUniqueEmailConstraint() {
        adapter.save(newUser("dup@example.com"));
        entityManager.flush();

        assertThatThrownBy(() -> {
            adapter.save(newUser("dup@example.com"));
            entityManager.flush();
        }).isInstanceOf(RuntimeException.class);
    }
}
