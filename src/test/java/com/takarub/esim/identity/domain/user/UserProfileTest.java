package com.takarub.esim.identity.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.takarub.esim.identity.domain.user.exception.UserDeletedException;
import com.takarub.esim.identity.shared.time.ClockProvider;

class UserProfileTest {

    private static final Instant CREATED = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant UPDATED = Instant.parse("2026-09-13T07:00:00Z");

    @Test
    void updateProfileStoresNormalizedValues() {
        User user = activeUser();
        ClockProvider clock = mock(ClockProvider.class);
        when(clock.now()).thenReturn(UPDATED);

        user.updateProfile(" Ahmad Ali ", "jo", "+962 790000000", clock);

        assertThat(user.fullName()).isEqualTo("Ahmad Ali");
        assertThat(user.country()).isEqualTo("JO");
        assertThat(user.phoneNumber()).isEqualTo("+962790000000");
        assertThat(user.updatedAt()).isEqualTo(UPDATED);
    }

    @Test
    void updateProfileRejectsDeletedUser() {
        User user = User.reconstitute(
                UserId.of(UUID.randomUUID()),
                CREATED,
                CREATED,
                EmailAddress.of("user@example.com"),
                PasswordHash.of("$2a$10$hashvalue"),
                UserStatus.DELETED,
                EnumSet.of(Role.CUSTOMER));

        assertThatThrownBy(() -> user.updateProfile("Ahmad", "JO", "+962790000000", mock(ClockProvider.class)))
                .isInstanceOf(UserDeletedException.class);
    }

    private static User activeUser() {
        return User.reconstitute(
                UserId.of(UUID.randomUUID()),
                CREATED,
                CREATED,
                EmailAddress.of("user@example.com"),
                PasswordHash.of("$2a$10$hashvalue"),
                UserStatus.ACTIVE,
                EnumSet.of(Role.CUSTOMER));
    }
}
