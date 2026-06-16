package com.takarub.esim.identity.infrastructure.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.stream.Collectors;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.takarub.esim.identity.application.port.AccessTokenIssuer;
import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.infrastructure.config.JwtProperties;
import com.takarub.esim.identity.shared.time.ClockProvider;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Issues signed JWT access tokens implementing the {@link AccessTokenIssuer} application port.
 */
@Component
public class JwtAccessTokenIssuer implements AccessTokenIssuer {

    private final JwtProperties properties;
    private final ClockProvider clock;
    private final SecretKey signingKey;

    public JwtAccessTokenIssuer(JwtProperties properties, ClockProvider clock) {
        this.properties = properties;
        this.clock = clock;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public String issueAccessToken(UserId userId, SessionId sessionId, Set<Role> roles, EmailAddress email) {
        Instant issuedAt = clock.now();
        Instant expiresAt = issuedAt.plus(properties.accessTokenExpiration());
        Set<String> roleNames = roles.stream().map(Role::name).collect(Collectors.toUnmodifiableSet());

        return Jwts.builder()
                .subject(userId.value().toString())
                .issuer(properties.issuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim(JwtClaimsNames.SESSION_ID, sessionId.value().toString())
                .claim(JwtClaimsNames.ROLES, roleNames)
                .claim("email", email.value())
                .signWith(signingKey)
                .compact();
    }
}
