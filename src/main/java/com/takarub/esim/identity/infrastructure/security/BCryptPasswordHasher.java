package com.takarub.esim.identity.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.takarub.esim.identity.application.port.PasswordHasher;
import com.takarub.esim.identity.domain.user.PasswordHash;

/**
 * BCrypt-based {@link PasswordHasher} implementation. The produced {@link PasswordHash} is an
 * opaque, self-describing bcrypt string (algorithm, cost and salt are encoded within it).
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Override
    public PasswordHash hash(String rawPassword) {
        return PasswordHash.of(passwordEncoder.encode(rawPassword));
    }

    @Override
    public boolean matches(String rawPassword, PasswordHash passwordHash) {
        return passwordEncoder.matches(rawPassword, passwordHash.value());
    }
}
