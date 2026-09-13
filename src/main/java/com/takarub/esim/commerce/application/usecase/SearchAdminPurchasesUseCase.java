package com.takarub.esim.commerce.application.usecase;

import org.springframework.data.domain.Page;

import com.takarub.esim.commerce.application.port.AdminPurchaseQueryPort;
import com.takarub.esim.commerce.application.query.SearchAdminPurchasesQuery;
import com.takarub.esim.commerce.application.result.AdminPurchasePage;
import com.takarub.esim.commerce.application.result.AdminPurchaseRow;

public class SearchAdminPurchasesUseCase {

    private final AdminPurchaseQueryPort adminPurchaseQueryPort;

    public SearchAdminPurchasesUseCase(AdminPurchaseQueryPort adminPurchaseQueryPort) {
        this.adminPurchaseQueryPort = adminPurchaseQueryPort;
    }

    public AdminPurchasePage execute(SearchAdminPurchasesQuery query) {
        Page<AdminPurchaseRow> page = adminPurchaseQueryPort.search(query);
        return new AdminPurchasePage(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
