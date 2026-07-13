package com.takarub.esim.pricing.domain.port;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Reads the cheapest in-stock normalized supplier cost for a catalog package.
 */
public interface CheapestNormalizedCostPort {

    Optional<BigDecimal> findCheapestInStockNormalizedCostUsd(String catalogPackageId);
}
