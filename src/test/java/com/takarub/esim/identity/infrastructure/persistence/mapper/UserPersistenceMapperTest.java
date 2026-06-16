package com.takarub.esim.identity.infrastructure.persistence.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.PasswordHash;
import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.User;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserStatus;
import com.takarub.esim.identity.infrastructure.persistence.entity.UserJpaEntity;

class UserPersistenceMapperTest {

    private final UserPersistenceMapper mapper = new UserPersistenceMapper();

    @Test
    void roundTripPreservesAllFields() {
        UserId id = UserId.of(UUID.randomUUID());
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant updatedAt = Instant.parse("2026-03-03T03:03:03Z");
        User user = User.reconstitute(id, createdAt, updatedAt, EmailAddress.of("user@example.com"),
                PasswordHash.of("$2a$10$hashvalue"), UserStatus.SUSPENDED, EnumSet.of(Role.CUSTOMER));

        UserJpaEntity entity = mapper.toEntity(user);

        assertThat(entity.getId()).isEqualTo(id.value().toString());
        assertThat(entity.getEmail()).isEqualTo("user@example.com");
        assertThat(entity.getPasswordHash()).isEqualTo("$2a$10$hashvalue");
        assertThat(entity.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(entity.getRoles()).containsExactly(Role.CUSTOMER);
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isEqualTo(updatedAt);

        User back = mapper.toDomain(entity);

        assertThat(back.id()).isEqualTo(id);
        assertThat(back.email()).isEqualTo(user.email());
        assertThat(back.passwordHash()).isEqualTo(user.passwordHash());
        assertThat(back.status()).isEqualTo(UserStatus.SUSPENDED);
        assertThat(back.roles()).containsExactly(Role.CUSTOMER);
        assertThat(back.createdAt()).isEqualTo(createdAt);
        assertThat(back.updatedAt()).isEqualTo(updatedAt);
    }
}
