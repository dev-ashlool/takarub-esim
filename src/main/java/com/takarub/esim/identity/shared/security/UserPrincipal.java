package com.takarub.esim.identity.shared.security;

import java.util.Set;

/**
 * Minimal, framework-agnostic view of the currently authenticated principal.
 *
 * <p>A pure shared contract &mdash; no Spring Security / JWT dependency.
 *
 * @param userId   stable user identifier
 * @param username human-readable username
 * @param roles    granted role names (never {@code null})
 */
public record UserPrincipal(String userId, String username, Set<String> roles) {

    public UserPrincipal {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}
