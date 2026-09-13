package com.takarub.esim.contact.infrastructure.persistence.adapter;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.takarub.esim.contact.domain.ContactMessage;
import com.takarub.esim.contact.domain.ContactMessageId;
import com.takarub.esim.contact.domain.ContactMessageRepository;
import com.takarub.esim.contact.infrastructure.persistence.mapper.ContactMessagePersistenceMapper;
import com.takarub.esim.contact.infrastructure.persistence.repository.ContactMessageJpaRepository;

@Component
public class ContactMessageRepositoryAdapter implements ContactMessageRepository {

    private final ContactMessageJpaRepository jpaRepository;
    private final ContactMessagePersistenceMapper mapper;

    public ContactMessageRepositoryAdapter(ContactMessageJpaRepository jpaRepository,
                                           ContactMessagePersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ContactMessage save(ContactMessage message) {
        return mapper.toDomain(jpaRepository.save(mapper.toEntity(message)));
    }

    @Override
    public Optional<ContactMessage> findById(ContactMessageId id) {
        return jpaRepository.findById(id.value().toString()).map(mapper::toDomain);
    }

    @Override
    public List<ContactMessage> findAllNewestFirst() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toDomain)
                .toList();
    }
}
