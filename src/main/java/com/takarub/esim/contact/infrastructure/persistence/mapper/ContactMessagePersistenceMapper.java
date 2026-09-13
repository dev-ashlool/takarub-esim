package com.takarub.esim.contact.infrastructure.persistence.mapper;

import org.springframework.stereotype.Component;

import com.takarub.esim.contact.domain.ContactMessage;
import com.takarub.esim.contact.domain.ContactMessageId;
import com.takarub.esim.contact.infrastructure.persistence.entity.ContactMessageJpaEntity;
import com.takarub.esim.identity.domain.user.EmailAddress;

@Component
public class ContactMessagePersistenceMapper {

    public ContactMessageJpaEntity toEntity(ContactMessage message) {
        return new ContactMessageJpaEntity(
                message.id().value().toString(),
                message.name(),
                message.email().value(),
                message.phoneNumber(),
                message.message(),
                message.status(),
                message.createdAt(),
                message.updatedAt());
    }

    public ContactMessage toDomain(ContactMessageJpaEntity entity) {
        return ContactMessage.reconstitute(
                ContactMessageId.of(entity.getId()),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getName(),
                EmailAddress.of(entity.getEmail()),
                entity.getPhoneNumber(),
                entity.getMessage(),
                entity.getStatus());
    }
}
