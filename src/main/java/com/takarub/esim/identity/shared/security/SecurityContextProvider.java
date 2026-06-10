package com.takarub.esim.identity.shared.security;

import java.util.Optional;

/**
 * Contract exposing the current security context to all layers without binding them to a concrete
 * security framework.
 *
 * <p>Implementations (Spring Security adapters, JWT, filters) are provided in later tasks; this
 * shared layer defines the contract only.
 */
public interface SecurityContextProvider {

    /**
     * @return the authenticated principal, if any
     */
    Optional<UserPrincipal> currentPrincipal();

    /**
     * @return the authenticated user id, if any
     */
    Optional<String> currentUserId();
}
