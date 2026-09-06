package com.takarub.esim.commerce.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.takarub.esim.commerce.domain.fulfillment.FulfillmentStatus;
import com.takarub.esim.commerce.infrastructure.persistence.entity.FulfillmentWorkJpaEntity;

public interface FulfillmentWorkJpaRepository extends JpaRepository<FulfillmentWorkJpaEntity, String> {

    Optional<FulfillmentWorkJpaEntity> findByOrderId(String orderId);

    @Query("""
            SELECT f.id FROM FulfillmentWorkJpaEntity f
            WHERE f.status = :pending
            ORDER BY f.createdAt ASC, f.id ASC
            """)
    List<String> findPendingIdsOrdered(
            @Param("pending") FulfillmentStatus pending,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FulfillmentWorkJpaEntity f
            SET f.status = :processing,
                f.claimedAt = :claimedAt,
                f.updatedAt = :updatedAt
            WHERE f.id = :id AND f.status = :pending
            """)
    int tryClaimPending(
            @Param("id") String id,
            @Param("pending") FulfillmentStatus pending,
            @Param("processing") FulfillmentStatus processing,
            @Param("claimedAt") Instant claimedAt,
            @Param("updatedAt") Instant updatedAt);

    @Query("""
            SELECT f.id FROM FulfillmentWorkJpaEntity f
            WHERE f.status = :processing AND f.claimedAt < :claimedBefore
            ORDER BY f.claimedAt ASC, f.id ASC
            """)
    List<String> findStaleProcessingIdsOrdered(
            @Param("processing") FulfillmentStatus processing,
            @Param("claimedBefore") Instant claimedBefore,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE FulfillmentWorkJpaEntity f
            SET f.status = :unknown,
                f.lastErrorCode = :errorCode,
                f.lastErrorMessage = :errorMessage,
                f.updatedAt = :updatedAt
            WHERE f.id = :id
              AND f.status = :processing
              AND f.claimedAt < :claimedBefore
            """)
    int tryMarkStaleProcessingUnknown(
            @Param("id") String id,
            @Param("processing") FulfillmentStatus processing,
            @Param("unknown") FulfillmentStatus unknown,
            @Param("claimedBefore") Instant claimedBefore,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("updatedAt") Instant updatedAt);
}
