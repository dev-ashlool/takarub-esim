package com.takarub.esim.identity.infrastructure.jwt;

/**
 * Stable JWT claim names used by the identity access-token issuer and validator.
 */
public final class JwtClaimsNames {

    public static final String SESSION_ID = "sid";
    public static final String ROLES = "roles";

    private JwtClaimsNames() {
    }
}
