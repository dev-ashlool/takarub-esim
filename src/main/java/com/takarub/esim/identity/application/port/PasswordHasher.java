package com.takarub.esim.identity.application.port;

import com.takarub.esim.identity.domain.user.PasswordHash;

/**
 * Application port for turning a raw password into a {@link PasswordHash}. Hashing strategy and
 * library are infrastructure concerns; the application depends only on this contract.
 *
 * <p>Implementations are provided by the infrastructure layer in a later task.
 */
public interface PasswordHasher {

    /**
     * Hashes a raw password.
     *
     * @param rawPassword the plaintext password supplied by the caller
     * @return the resulting {@link PasswordHash}
     */
    PasswordHash hash(String rawPassword);
}
