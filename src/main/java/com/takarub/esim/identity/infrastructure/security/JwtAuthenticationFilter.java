package com.takarub.esim.identity.infrastructure.security;

import java.io.IOException;

import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.infrastructure.audit.LoggingAuditEventRecorder;
import com.takarub.esim.identity.infrastructure.jwt.JwtAccessTokenValidator;
import com.takarub.esim.identity.infrastructure.jwt.ValidatedJwtClaims;
import com.takarub.esim.identity.shared.security.UserPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Servlet filter that validates bearer JWT access tokens and populates the security context.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtAccessTokenValidator jwtAccessTokenValidator;
    private final AuthenticatedSessionValidator authenticatedSessionValidator;
    private final LoggingAuditEventRecorder auditEventRecorder;

    public JwtAuthenticationFilter(JwtAccessTokenValidator jwtAccessTokenValidator,
                                   AuthenticatedSessionValidator authenticatedSessionValidator,
                                   LoggingAuditEventRecorder auditEventRecorder) {
        this.jwtAccessTokenValidator = jwtAccessTokenValidator;
        this.authenticatedSessionValidator = authenticatedSessionValidator;
        this.auditEventRecorder = auditEventRecorder;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        ValidatedJwtClaims claims = null;
        try {
            claims = jwtAccessTokenValidator.validate(token);
            authenticatedSessionValidator.validateActiveSession(SessionId.of(claims.sessionId()));

            String username = claims.email() != null ? claims.email() : claims.userId();
            UserPrincipal principal = new UserPrincipal(claims.userId(), username, claims.roles());
            SecurityContextHolder.getContext().setAuthentication(
                    new UserPrincipalAuthenticationToken(principal));
            filterChain.doFilter(request, response);
        } catch (RuntimeException ex) {
            SecurityContextHolder.clearContext();
            auditEventRecorder.recordTokenValidationFailure(
                    claims != null ? claims.userId() : null,
                    request.getRequestURI());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, resolveMessage(ex));
        }
    }

    private static String resolveMessage(RuntimeException ex) {
        return ex.getMessage() != null ? ex.getMessage() : "Unauthorized";
    }
}
