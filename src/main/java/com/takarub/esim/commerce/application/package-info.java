/**
 * Commerce <strong>application</strong> layer &mdash; cart use cases / orchestration.
 *
 * <p>Depends on the commerce cart domain and collaborates with Catalog (package reads) and Pricing
 * (sell price, via catalog browse). MUST NOT contain cart business rules; the {@code Cart}
 * aggregate decides. Infrastructure wiring arrives in a later task.
 */
package com.takarub.esim.commerce.application;
