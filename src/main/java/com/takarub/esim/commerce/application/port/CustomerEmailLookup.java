package com.takarub.esim.commerce.application.port;

import java.util.Optional;

import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.UserId;

/**
 * Resolves the customer mailbox for commerce notifications without commerce depending on identity
 * JPA.
 */
public interface CustomerEmailLookup {

    Optional<EmailAddress> findEmailByUserId(UserId userId);
}
