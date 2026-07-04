CREATE TABLE email_smtp_config (
    id         BIGINT       AUTO_INCREMENT PRIMARY KEY,
    host       VARCHAR(255) NOT NULL,
    port       INT          NOT NULL,
    username   VARCHAR(255) NOT NULL,
    password   VARCHAR(512) NOT NULL,
    auth_enabled BOOLEAN    NOT NULL DEFAULT TRUE,
    tls_enabled  BOOLEAN    NOT NULL DEFAULT TRUE,
    from_email VARCHAR(255) NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);
