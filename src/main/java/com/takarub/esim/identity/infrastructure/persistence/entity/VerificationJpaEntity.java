package com.takarub.esim.identity.infrastructure.persistence.entity;

import java.time.Instant;

import com.takarub.esim.identity.domain.verification.VerificationStatus;
import com.takarub.esim.identity.domain.verification.VerificationType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA persistence representation of the Verification aggregate. References the owning user by its
 * UUID string only.
 */
@Entity
@Table(name = "verifications")
public class VerificationJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false, updatable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 32, nullable = false, updatable = false)
    private VerificationType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private VerificationStatus status;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected VerificationJpaEntity() {
        // Required by JPA.
    }

    public VerificationJpaEntity(String id, String userId, VerificationType type,
                                 VerificationStatus status, Instant expiresAt, Instant createdAt,
                                 Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public VerificationType getType() {
        return type;
    }

    public VerificationStatus getStatus() {
        return status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
