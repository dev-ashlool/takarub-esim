package com.takarub.esim.identity.domain.user;

import java.util.Optional;

/**
 * Repository port for the User aggregate. Implementations live in the infrastructure layer (not in
 * this task).
 */
public interface UserRepository {

    User save(User user);

    Optional<User> findById(UserId userId);

    Optional<User> findByEmail(EmailAddress email);

    boolean existsByEmail(EmailAddress email);
}
