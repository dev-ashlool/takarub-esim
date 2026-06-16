package com.takarub.esim.identity.infrastructure.jwt;

import java.util.Set;

/**
 * Validated access-token claims exposed to security infrastructure components.
 */
public record ValidatedJwtClaims(
        String userId,
        String sessionId,
        String email,
        Set<String> roles
) {
}
