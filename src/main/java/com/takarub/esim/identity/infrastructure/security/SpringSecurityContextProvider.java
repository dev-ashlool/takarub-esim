package com.takarub.esim.identity.infrastructure.security;

import java.util.Optional;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.takarub.esim.identity.shared.security.SecurityContextProvider;
import com.takarub.esim.identity.shared.security.UserPrincipal;

/**
 * Infrastructure adapter exposing the current {@link UserPrincipal} from Spring Security's
 * {@link SecurityContextHolder}.
 */
@Component
public class SpringSecurityContextProvider implements SecurityContextProvider {

    @Override
    public Optional<UserPrincipal> currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof UserPrincipal userPrincipal) {
            return Optional.of(userPrincipal);
        }
        return Optional.empty();
    }

    @Override
    public Optional<String> currentUserId() {
        return currentPrincipal().map(UserPrincipal::userId);
    }
}
