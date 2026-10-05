--liquibase formatted sql
--changeset student:2
ALTER TABLE tasks ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

--rollback ALTER TABLE tasks DROP COLUMN created_at;
