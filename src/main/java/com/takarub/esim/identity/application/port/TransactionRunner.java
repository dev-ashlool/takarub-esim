package com.takarub.esim.identity.application.port;

import java.util.function.Supplier;

/**
 * Application port for running a unit of work inside a single transaction boundary. The use case
 * owns the boundary; the transaction technology is an infrastructure concern provided by an
 * implementation of this contract.
 *
 * <p>The supplied work either completes and its result is returned (commit), or it throws an
 * unchecked exception that propagates after the transaction is rolled back.
 */
public interface TransactionRunner {

    /**
     * Executes {@code work} within a transaction and returns its result.
     *
     * @param work the unit of work to run transactionally
     * @param <T>  the result type
     * @return the value produced by {@code work}
     */
    <T> T execute(Supplier<T> work);
}
