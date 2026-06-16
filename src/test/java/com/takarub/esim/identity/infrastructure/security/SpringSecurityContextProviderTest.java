package com.takarub.esim.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.takarub.esim.identity.shared.security.UserPrincipal;

class SpringSecurityContextProviderTest {

    private final SpringSecurityContextProvider provider = new SpringSecurityContextProvider();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void returnsAuthenticatedPrincipal() {
        UserPrincipal principal = new UserPrincipal("user-id", "user@example.com", Set.of("CUSTOMER"));
        SecurityContextHolder.getContext().setAuthentication(
                new UserPrincipalAuthenticationToken(principal, "session-id"));

        assertThat(provider.currentPrincipal()).contains(principal);
        assertThat(provider.currentUserId()).contains("user-id");
        assertThat(provider.currentSessionId()).contains("session-id");
    }

    @Test
    void returnsEmptyForAnonymousContext() {
        assertThat(provider.currentPrincipal()).isEmpty();
        assertThat(provider.currentUserId()).isEmpty();
        assertThat(provider.currentSessionId()).isEmpty();
    }
}
