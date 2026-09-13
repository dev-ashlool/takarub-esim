-- Public Contact Us submissions. Status is owned by admin workflows.

CREATE TABLE contact_messages (
    id           VARCHAR(36)   NOT NULL,
    name         VARCHAR(120)  NOT NULL,
    email        VARCHAR(320)  NOT NULL,
    phone_number VARCHAR(20)   NULL,
    message      VARCHAR(2000) NOT NULL,
    status       VARCHAR(32)   NOT NULL,
    created_at   DATETIME(6)   NOT NULL,
    updated_at   DATETIME(6)   NOT NULL,
    CONSTRAINT pk_contact_messages PRIMARY KEY (id)
);

CREATE INDEX ix_contact_messages_status_created_at ON contact_messages (status, created_at);
CREATE INDEX ix_contact_messages_created_at ON contact_messages (created_at);
