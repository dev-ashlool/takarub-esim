package com.takarub.esim.catalog.infrastructure.persistence;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryJpaRepository extends JpaRepository<CountryEntity, String> {

    Optional<CountryEntity> findBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCase(String slug);
}
