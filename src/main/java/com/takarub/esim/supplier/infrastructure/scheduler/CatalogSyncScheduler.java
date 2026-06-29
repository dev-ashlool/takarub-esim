package com.takarub.esim.supplier.infrastructure.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.takarub.esim.supplier.application.command.SyncSupplierCatalogCommand;
import com.takarub.esim.supplier.application.result.SyncSupplierCatalogResult;
import com.takarub.esim.supplier.application.usecases.SyncSupplierCatalogUseCase;

/**
 * Background worker that synchronizes the LikeCard catalog twice per day.
 */
@Component
public class CatalogSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(CatalogSyncScheduler.class);

    private final SyncSupplierCatalogUseCase syncSupplierCatalogUseCase;

    public CatalogSyncScheduler(SyncSupplierCatalogUseCase syncSupplierCatalogUseCase) {
        this.syncSupplierCatalogUseCase = syncSupplierCatalogUseCase;
    }

    @Scheduled(cron = "0 0 */12 * * ?")
    public void syncLikeCardCatalog() {
        log.info("Starting scheduled LikeCard catalog synchronization");
        SyncSupplierCatalogResult result =
                syncSupplierCatalogUseCase.execute(SyncSupplierCatalogCommand.forLikeCard());
        log.info(
                "Completed scheduled LikeCard catalog synchronization: supplierKey={}, fetched={}, upserted={}, outOfStock={}, unavailable={}",
                result.supplierKey(),
                result.productsFetched(),
                result.mappingsUpserted(),
                result.mappingsMarkedOutOfStock(),
                result.catalogPackagesMarkedUnavailable());
    }
}
