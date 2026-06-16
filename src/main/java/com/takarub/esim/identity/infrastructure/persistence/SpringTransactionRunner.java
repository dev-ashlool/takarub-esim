package com.takarub.esim.identity.infrastructure.persistence;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.takarub.esim.identity.application.port.TransactionRunner;

/**
 * Infrastructure implementation of the {@link TransactionRunner} application port using Spring's
 * programmatic {@link TransactionTemplate}. Opens a transaction around the supplied work; repository
 * operations invoked inside join this transaction and commit or roll back together. The transaction
 * rolls back automatically when the work throws an unchecked exception.
 */
@Component
public class SpringTransactionRunner implements TransactionRunner {

    private final TransactionTemplate transactionTemplate;

    public SpringTransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T execute(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }
}
