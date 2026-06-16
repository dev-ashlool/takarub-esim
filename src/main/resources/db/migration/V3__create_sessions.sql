CREATE TABLE sessions (
    id               VARCHAR(36)  NOT NULL,
    user_id          VARCHAR(36)  NOT NULL,
    refresh_token    VARCHAR(255) NOT NULL,
    status           VARCHAR(32)  NOT NULL,
    device_name      VARCHAR(255) NOT NULL,
    device_type      VARCHAR(64)  NOT NULL,
    ip_address       VARCHAR(45)  NOT NULL,
    user_agent       VARCHAR(512) NOT NULL,
    last_activity_at DATETIME(6)  NOT NULL,
    expires_at       DATETIME(6)  NOT NULL,
    created_at       DATETIME(6)  NOT NULL,
    updated_at       DATETIME(6)  NOT NULL,
    CONSTRAINT pk_sessions PRIMARY KEY (id),
    CONSTRAINT fk_sessions_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX ix_sessions_user_id_status ON sessions (user_id, status);
