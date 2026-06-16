package com.takarub.esim.identity.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.takarub.esim.identity.domain.user.EmailAddress;
import com.takarub.esim.identity.domain.user.PasswordHash;
import com.takarub.esim.identity.domain.user.User;
import com.takarub.esim.identity.domain.user.UserId;
import com.takarub.esim.identity.infrastructure.persistence.entity.UserJpaEntity;

/**
 * Translates between the {@link User} aggregate and {@link UserJpaEntity}. The reverse direction
 * uses the domain's explicit {@link User#reconstitute} factory; no reflection is used.
 */
@Component
public class UserPersistenceMapper {

    public UserJpaEntity toEntity(User user) {
        return new UserJpaEntity(
                user.id().value().toString(),
                user.email().value(),
                user.passwordHash().value(),
                user.status(),
                user.roles(),
                user.createdAt(),
                user.updatedAt());
    }

    public User toDomain(UserJpaEntity entity) {
        return User.reconstitute(
                UserId.of(entity.getId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                EmailAddress.of(entity.getEmail()),
                PasswordHash.of(entity.getPasswordHash()),
                entity.getStatus(),
                entity.getRoles());
    }
}
