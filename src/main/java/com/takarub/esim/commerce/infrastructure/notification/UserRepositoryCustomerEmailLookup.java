package com.takarub.esim.commerce.infrastructure.notification;

import java.util.Optional;

import com.takarub.esim.commerce.application.port.CustomerEmailLookup;
import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.domain.user.UserRepository;

/**
 * Looks up customer email via the identity domain {@link UserRepository} port.
 */
public class UserRepositoryCustomerEmailLookup implements CustomerEmailLookup {

    private final UserRepository userRepository;

    public UserRepositoryCustomerEmailLookup(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<EmailAddress> findEmailByUserId(UserId userId) {
        return userRepository.findById(userId).map(user -> user.email());
    }
}
