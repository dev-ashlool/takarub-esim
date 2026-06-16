CREATE TABLE verifications (
    id         VARCHAR(36) NOT NULL,
    user_id    VARCHAR(36) NOT NULL,
    type       VARCHAR(32) NOT NULL,
    status     VARCHAR(32) NOT NULL,
    expires_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    CONSTRAINT pk_verifications PRIMARY KEY (id),
    CONSTRAINT fk_verifications_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX ix_verifications_user_id_type_status ON verifications (user_id, type, status);
