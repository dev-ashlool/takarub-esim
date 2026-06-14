package com.takarub.esim.identity.infrastructure.persistence.entity;

import java.time.Instant;

import com.takarub.esim.identity.domain.session.SessionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * JPA persistence representation of the Session aggregate. The refresh token is stored as-is (no
 * hashing in this task). References the owning user by its UUID string only.
 */
@Entity
@Table(name = "sessions")
public class SessionJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "user_id", length = 36, nullable = false, updatable = false)
    private String userId;

    @Column(name = "refresh_token", length = 255, nullable = false)
    private String refreshToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private SessionStatus status;

    @Embedded
    private DeviceMetadataEmbeddable deviceMetadata;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SessionJpaEntity() {
        // Required by JPA.
    }

    public SessionJpaEntity(String id, String userId, String refreshToken, SessionStatus status,
                            DeviceMetadataEmbeddable deviceMetadata, Instant expiresAt,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.refreshToken = refreshToken;
        this.status = status;
        this.deviceMetadata = deviceMetadata;
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

    public String getRefreshToken() {
        return refreshToken;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public DeviceMetadataEmbeddable getDeviceMetadata() {
        return deviceMetadata;
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
