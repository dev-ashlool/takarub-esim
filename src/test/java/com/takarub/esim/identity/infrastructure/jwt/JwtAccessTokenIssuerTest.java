package com.takarub.esim.identity.infrastructure.jwt;

import static org.assertj.core.api.Assertions.assertThat;

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

import java.util.Date;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

class JwtAccessTokenIssuerTest {

    private static final String SECRET = "test-jwt-secret-must-be-at-least-32-chars-long";
    private static final Instant FIXED = Instant.parse("2026-06-16T10:00:00Z");

    private JwtAccessTokenIssuer issuer;
    private JwtProperties properties;

    @BeforeEach
    void setUp() {
        properties = new JwtProperties(SECRET, "takarub-esim-identity-test", Duration.ofMinutes(15));
        issuer = new JwtAccessTokenIssuer(properties,
                new SystemClockProvider(Clock.fixed(FIXED, ZoneOffset.UTC)));
    }

    @Test
    void generatesTokenWithRequiredClaims() {
        UserId userId = UserId.of(UUID.randomUUID());
        SessionId sessionId = SessionId.of(UUID.randomUUID());

        String token = issuer.issueAccessToken(userId, sessionId, EnumSet.of(Role.ADMIN),
                EmailAddress.of("user@example.com"));

        Claims claims = parseClaims(token);
        assertThat(claims.getSubject()).isEqualTo(userId.value().toString());
        assertThat(claims.getIssuer()).isEqualTo(properties.issuer());
        assertThat(claims.get(JwtClaimsNames.SESSION_ID, String.class))
                .isEqualTo(sessionId.value().toString());
        assertThat(claims.get("email", String.class)).isEqualTo("user@example.com");
        assertThat(claims.get(JwtClaimsNames.ROLES)).asList().containsExactly("ADMIN");
        assertThat(claims.getIssuedAt().toInstant()).isEqualTo(FIXED);
        assertThat(claims.getExpiration().toInstant()).isEqualTo(FIXED.plus(properties.accessTokenExpiration()));
    }

    @SuppressWarnings("unchecked")
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes()))
                .clock(() -> Date.from(FIXED.plus(Duration.ofMinutes(5))))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
