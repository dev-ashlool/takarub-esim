package com.takarub.esim.identity.infrastructure.email;

/**
 * Thrown when the system attempts to send an email but no active SMTP configuration
 * exists in the database. The administrator must configure SMTP via the admin API first.
 */
public class NoActiveSmtpConfigException extends RuntimeException {

    public NoActiveSmtpConfigException() {
        super("No active SMTP configuration found. "
                + "Please configure SMTP settings via PUT /api/v1/admin/email/smtp-config");
    }
}
