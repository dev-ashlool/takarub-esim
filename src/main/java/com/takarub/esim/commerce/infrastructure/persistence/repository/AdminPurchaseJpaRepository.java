package com.takarub.esim.commerce.infrastructure.persistence.repository;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.takarub.esim.commerce.application.result.AdminPurchaseRow;
import com.takarub.esim.commerce.infrastructure.persistence.entity.OrderJpaEntity;

public interface AdminPurchaseJpaRepository extends JpaRepository<OrderJpaEntity, String> {

    @Query("""
            select new com.takarub.esim.commerce.application.result.AdminPurchaseRow(
                o.id,
                o.status,
                o.createdAt,
                o.totalAmount,
                o.currency,
                i.packageId,
                i.countryIso,
                i.countryNameEnglish,
                i.dataAmount,
                i.dataUnit,
                i.durationDays,
                i.supplierKey,
                u.email,
                u.fullName,
                u.phoneNumber)
            from OrderJpaEntity o
            join o.items i
            join com.takarub.esim.identity.infrastructure.persistence.entity.UserJpaEntity u
                on u.id = o.userId
            where (:supplierKey is null or :supplierKey = '' or i.supplierKey = :supplierKey)
              and (:from is null or o.createdAt >= :from)
              and (:to is null or o.createdAt <= :to)
              and (:packageName is null or :packageName = ''
                   or lower(i.countryNameEnglish) like lower(concat('%', :packageName, '%'))
                   or lower(i.countryNameArabic) like lower(concat('%', :packageName, '%')))
              and (:email is null or :email = '' or lower(u.email) like lower(concat('%', :email, '%')))
              and (:phone is null or :phone = '' or u.phoneNumber like concat('%', :phone, '%'))
              and (:name is null or :name = '' or lower(u.fullName) like lower(concat('%', :name, '%')))
            """)
    Page<AdminPurchaseRow> search(
            @Param("supplierKey") String supplierKey,
            @Param("from") Instant from,
            @Param("to") Instant to,
            @Param("packageName") String packageName,
            @Param("phone") String phone,
            @Param("email") String email,
            @Param("name") String name,
            Pageable pageable);
}
