package com.takarub.esim.identity.infrastructure.persistence.entity;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import com.takarub.esim.identity.domain.user.Role;
import com.takarub.esim.identity.domain.user.UserStatus;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

/**
 * JPA persistence representation of the User aggregate. Lives entirely in the infrastructure layer;
 * the domain aggregate carries no persistence annotations. Identifiers are stored as their canonical
 * UUID string form.
 */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @Column(name = "id", length = 36, nullable = false, updatable = false)
    private String id;

    @Column(name = "email", length = 320, nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", length = 255, nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 32, nullable = false)
    private UserStatus status;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 32, nullable = false)
    private Set<Role> roles = new HashSet<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "full_name", length = 120)
    private String fullName;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    protected UserJpaEntity() {
        // Required by JPA.
    }

    public UserJpaEntity(String id, String email, String passwordHash, UserStatus status,
                         Set<Role> roles, Instant createdAt, Instant updatedAt) {
        this(id, email, passwordHash, status, roles, createdAt, updatedAt, null, null, null);
    }

    public UserJpaEntity(String id, String email, String passwordHash, UserStatus status,
                         Set<Role> roles, Instant createdAt, Instant updatedAt,
                         String fullName, String country, String phoneNumber) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.status = status;
        this.roles = new HashSet<>(roles);
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.fullName = fullName;
        this.country = country;
        this.phoneNumber = phoneNumber;
    }

    public String getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Set<Role> getRoles() {
        return roles;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getFullName() {
        return fullName;
    }

    public String getCountry() {
        return country;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }
}
