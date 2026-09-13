package com.takarub.esim.commerce.application.port;

import org.springframework.data.domain.Page;

import com.takarub.esim.commerce.application.query.SearchAdminPurchasesQuery;
import com.takarub.esim.commerce.application.result.AdminPurchaseRow;

public interface AdminPurchaseQueryPort {

    Page<AdminPurchaseRow> search(SearchAdminPurchasesQuery query);
}
