package com.takarub.esim.identity.infrastructure.email;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SmtpConfigJpaRepository extends JpaRepository<SmtpConfigEntity, Long> {

    Optional<SmtpConfigEntity> findByActiveTrue();
}
