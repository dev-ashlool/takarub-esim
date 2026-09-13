package com.takarub.esim.commerce.infrastructure.persistence.adapter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.takarub.esim.commerce.application.port.AdminPurchaseQueryPort;
import com.takarub.esim.commerce.application.query.SearchAdminPurchasesQuery;
import com.takarub.esim.commerce.application.result.AdminPurchaseRow;
import com.takarub.esim.commerce.infrastructure.persistence.repository.AdminPurchaseJpaRepository;

@Component
public class AdminPurchaseQueryAdapter implements AdminPurchaseQueryPort {

    private final AdminPurchaseJpaRepository adminPurchaseJpaRepository;

    public AdminPurchaseQueryAdapter(AdminPurchaseJpaRepository adminPurchaseJpaRepository) {
        this.adminPurchaseJpaRepository = adminPurchaseJpaRepository;
    }

    @Override
    public Page<AdminPurchaseRow> search(SearchAdminPurchasesQuery query) {
        int page = Math.max(query.page(), 0);
        int size = query.size() <= 0 ? 20 : Math.min(query.size(), 100);
        return adminPurchaseJpaRepository.search(
                blankToNull(query.supplierKey()),
                query.from(),
                query.to(),
                blankToNull(query.packageName()),
                blankToNull(query.phone()),
                blankToNull(query.email()),
                blankToNull(query.name()),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
