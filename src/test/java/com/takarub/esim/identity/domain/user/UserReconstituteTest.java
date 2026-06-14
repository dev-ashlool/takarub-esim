package com.takarub.esim.identity.domain.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class UserReconstituteTest {

    @Test
    void restoresAllStateIncludingDistinctUpdatedAtAndTerminalStatus() {
        UserId id = UserId.of(UUID.randomUUID());
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-02-02T12:30:00Z");
        EmailAddress email = EmailAddress.of("user@example.com");
        PasswordHash hash = PasswordHash.of("$2a$10$abcdefghijklmnopqrstuv");

        User user = User.reconstitute(id, createdAt, updatedAt, email, hash, UserStatus.DELETED,
                EnumSet.of(Role.ADMIN, Role.CUSTOMER));

        assertThat(user.id()).isEqualTo(id);
        assertThat(user.createdAt()).isEqualTo(createdAt);
        assertThat(user.updatedAt()).isEqualTo(updatedAt);
        assertThat(user.email()).isEqualTo(email);
        assertThat(user.passwordHash()).isEqualTo(hash);
        assertThat(user.status()).isEqualTo(UserStatus.DELETED);
        assertThat(user.roles()).containsExactlyInAnyOrder(Role.ADMIN, Role.CUSTOMER);
    }

    @Test
    void restoresUserWithNoRoles() {
        User user = User.reconstitute(UserId.of(UUID.randomUUID()),
                Instant.parse("2026-01-01T00:00:00Z"), Instant.parse("2026-01-01T00:00:00Z"),
                EmailAddress.of("a@b.com"), PasswordHash.of("hash"), UserStatus.ACTIVE, Set.of());

        assertThat(user.roles()).isEmpty();
        assertThat(user.status()).isEqualTo(UserStatus.ACTIVE);
    }
}
