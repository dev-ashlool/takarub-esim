package com.takarub.esim.identity.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.infrastructure.config.JwtProperties;
import com.takarub.esim.identity.shared.exception.UnauthorizedException;
import com.takarub.esim.identity.shared.time.SystemClockProvider;

class JwtAccessTokenValidatorTest {

    private static final String SECRET = "test-jwt-secret-must-be-at-least-32-chars-long";
    private static final Instant FIXED = Instant.parse("2026-06-16T10:00:00Z");

    private JwtAccessTokenIssuer issuer;
    private JwtAccessTokenValidator validator;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties(SECRET, "takarub-esim-identity-test",
                Duration.ofMinutes(15));
        SystemClockProvider clock = new SystemClockProvider(Clock.fixed(FIXED, ZoneOffset.UTC));
        issuer = new JwtAccessTokenIssuer(properties, clock);
        validator = new JwtAccessTokenValidator(properties);
    }

    @Test
    void validatesWellFormedToken() {
        SystemClockProvider clock = new SystemClockProvider(Clock.systemUTC());
        JwtProperties properties = new JwtProperties(SECRET, "takarub-esim-identity-test",
                Duration.ofMinutes(15));
        JwtAccessTokenIssuer liveIssuer = new JwtAccessTokenIssuer(properties, clock);
        JwtAccessTokenValidator liveValidator = new JwtAccessTokenValidator(properties);

        UserId userId = UserId.of(UUID.randomUUID());
        SessionId sessionId = SessionId.of(UUID.randomUUID());
        String token = liveIssuer.issueAccessToken(userId, sessionId, EnumSet.of(Role.CUSTOMER),
                EmailAddress.of("user@example.com"));

        ValidatedJwtClaims claims = liveValidator.validate(token);

        assertThat(claims.userId()).isEqualTo(userId.value().toString());
        assertThat(claims.sessionId()).isEqualTo(sessionId.value().toString());
        assertThat(claims.email()).isEqualTo("user@example.com");
        assertThat(claims.roles()).containsExactly("CUSTOMER");
    }

    @Test
    void rejectsInvalidSignature() {
        UserId userId = UserId.of(UUID.randomUUID());
        SessionId sessionId = SessionId.of(UUID.randomUUID());
        String token = issuer.issueAccessToken(userId, sessionId, EnumSet.of(Role.CUSTOMER),
                EmailAddress.of("user@example.com")) + "tampered";

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("invalid");
    }

    @Test
    void rejectsExpiredToken() {
        JwtProperties expiredProperties = new JwtProperties(SECRET, "takarub-esim-identity-test",
                Duration.ofMinutes(1));
        Instant past = Instant.parse("2020-01-01T00:00:00Z");
        JwtAccessTokenIssuer pastIssuer = new JwtAccessTokenIssuer(expiredProperties,
                new SystemClockProvider(Clock.fixed(past, ZoneOffset.UTC)));
        String token = pastIssuer.issueAccessToken(UserId.of(UUID.randomUUID()),
                SessionId.of(UUID.randomUUID()), EnumSet.of(Role.CUSTOMER),
                EmailAddress.of("user@example.com"));

        assertThatThrownBy(() -> validator.validate(token))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
    }
}
