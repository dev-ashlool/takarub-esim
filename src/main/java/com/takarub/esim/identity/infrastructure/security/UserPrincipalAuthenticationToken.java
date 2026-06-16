package com.takarub.esim.identity.infrastructure.security;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.shared.security.UserPrincipal;

/**
 * Spring Security authentication token carrying a framework-agnostic {@link UserPrincipal}.
 */
public class UserPrincipalAuthenticationToken extends AbstractAuthenticationToken {

    private final UserPrincipal principal;
    private final String sessionId;

    public UserPrincipalAuthenticationToken(UserPrincipal principal, String sessionId) {
        super(toAuthorities(principal.roles()));
        this.principal = principal;
        this.sessionId = sessionId;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return "";
    }

    @Override
    public UserPrincipal getPrincipal() {
        return principal;
    }

    public String sessionId() {
        return sessionId;
    }

    private static Collection<? extends GrantedAuthority> toAuthorities(Set<String> roles) {
        return roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toUnmodifiableSet());
    }

    public static Set<String> roleNamesFromDomain(Set<Role> roles) {
        return roles.stream().map(Role::name).collect(Collectors.toUnmodifiableSet());
    }
}
