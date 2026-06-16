package com.takarub.esim.identity.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.infrastructure.audit.LoggingAuditEventRecorder;
import com.takarub.esim.identity.infrastructure.jwt.JwtAccessTokenValidator;
import com.takarub.esim.identity.infrastructure.jwt.ValidatedJwtClaims;
import com.takarub.esim.identity.shared.exception.UnauthorizedException;
import com.takarub.esim.identity.shared.security.UserPrincipal;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtAccessTokenValidator jwtAccessTokenValidator;
    @Mock
    private AuthenticatedSessionValidator authenticatedSessionValidator;
    @Mock
    private LoggingAuditEventRecorder auditEventRecorder;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtAccessTokenValidator, authenticatedSessionValidator,
                auditEventRecorder);
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesValidBearerToken() throws Exception {
        String userId = UUID.randomUUID().toString();
        String sessionId = UUID.randomUUID().toString();
        ValidatedJwtClaims claims = new ValidatedJwtClaims(userId, sessionId, "user@example.com",
                Set.of("CUSTOMER"));
        when(jwtAccessTokenValidator.validate("valid-token")).thenReturn(claims);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal();
        assertThat(principal.userId()).isEqualTo(userId);
        UserPrincipalAuthenticationToken authentication =
                (UserPrincipalAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication.sessionId()).isEqualTo(sessionId);
        verify(authenticatedSessionValidator).validateActiveSession(SessionId.of(sessionId));
    }

    @Test
    void rejectsInvalidToken() throws Exception {
        when(jwtAccessTokenValidator.validate("bad-token"))
                .thenThrow(new UnauthorizedException("Access token is invalid."));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer bad-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(authenticatedSessionValidator, never()).validateActiveSession(any());
        verify(auditEventRecorder).recordTokenValidationFailure(null, request.getRequestURI());
    }

    @Test
    void continuesChainWhenTokenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtAccessTokenValidator, never()).validate(any());
    }

    @Test
    void rejectsWhenSessionValidationFails() throws Exception {
        String sessionId = UUID.randomUUID().toString();
        ValidatedJwtClaims claims = new ValidatedJwtClaims(UUID.randomUUID().toString(), sessionId,
                "user@example.com", Set.of("CUSTOMER"));
        when(jwtAccessTokenValidator.validate("valid-token")).thenReturn(claims);
        doThrow(new UnauthorizedException("Session no longer exists."))
                .when(authenticatedSessionValidator).validateActiveSession(SessionId.of(sessionId));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(401);
    }
}
