package com.takarub.esim.identity.application.port;

import java.util.Set;

import com.takarub.esim.identity.domain.session.SessionId;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.UserId;

/**
 * Application port for issuing opaque access tokens after successful authentication. The token
 * format and signing are infrastructure concerns; this contract exposes only domain-friendly types.
 */
public interface AccessTokenIssuer {

    /**
     * Issues a new access token for an authenticated session.
     *
     * @param userId    the authenticated user
     * @param sessionId the active session backing the authentication
     * @param roles     granted roles at issuance time
     * @param email     the user's e-mail (used as the human-readable principal name)
     * @return the access token string to return to the caller
     */
    String issueAccessToken(UserId userId, SessionId sessionId, Set<Role> roles, EmailAddress email);
}
