package com.takarub.esim.contact.infrastructure.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.takarub.esim.contact.infrastructure.persistence.entity.ContactMessageJpaEntity;

public interface ContactMessageJpaRepository extends JpaRepository<ContactMessageJpaEntity, String> {

    List<ContactMessageJpaEntity> findAllByOrderByCreatedAtDesc();
}
