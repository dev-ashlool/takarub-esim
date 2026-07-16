package com.takarub.esim.catalog.application.query;

/**
 * Query to retrieve a single catalog package by its identifier.
 */
public record GetPackageDetailsQuery(String packageId) {
}
