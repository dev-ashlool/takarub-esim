/**
 * Commerce order domain: commercial purchase aggregate created from a checked-out cart snapshot.
 *
 * <p>Does not load Catalog, Pricing, Payment providers, or Cart repositories. Application layers
 * supply {@link com.takarub.esim.commerce.domain.order.OrderItemSnapshot} data and drive lifecycle
 * transitions.
 */
package com.takarub.esim.commerce.domain.order;
