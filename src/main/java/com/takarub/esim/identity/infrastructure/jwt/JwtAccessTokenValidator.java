package com.takarub.esim.identity.infrastructure.jwt;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.takarub.esim.identity.infrastructure.config.JwtProperties;
import com.takarub.esim.identity.shared.exception.UnauthorizedException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

/**
 * Parses and cryptographically validates JWT access tokens.
 */
@Component
public class JwtAccessTokenValidator {

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtAccessTokenValidator(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public ValidatedJwtClaims validate(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.issuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String userId = claims.getSubject();
            String sessionId = claims.get(JwtClaimsNames.SESSION_ID, String.class);
            String email = claims.get("email", String.class);
            Set<String> roles = extractRoles(claims);

            if (userId == null || userId.isBlank() || sessionId == null || sessionId.isBlank()) {
                throw new UnauthorizedException("Access token is missing required claims.");
            }

            return new ValidatedJwtClaims(userId, sessionId, email, roles);
        } catch (ExpiredJwtException ex) {
            throw new UnauthorizedException("Access token has expired.");
        } catch (SignatureException ex) {
            throw new UnauthorizedException("Access token signature is invalid.");
        } catch (JwtException ex) {
            throw new UnauthorizedException("Access token is invalid.");
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<String> extractRoles(Claims claims) {
        Object rawRoles = claims.get(JwtClaimsNames.ROLES);
        if (rawRoles instanceof List<?> list) {
            Set<String> roles = new LinkedHashSet<>();
            for (Object item : list) {
                if (item != null) {
                    roles.add(item.toString());
                }
            }
            return Set.copyOf(roles);
        }
        return Set.of();
    }
}
