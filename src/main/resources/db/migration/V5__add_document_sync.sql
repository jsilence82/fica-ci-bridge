ALTER TABLE invoices ADD COLUMN last_synced_at TIMESTAMP;

-- One row per DocumentSyncScheduler run: interview/demo evidence today, a
-- Prometheus source (staleness, failure rate) later.
CREATE TABLE sync_run (
    id                 BIGSERIAL       PRIMARY KEY,
    started_at         TIMESTAMP       NOT NULL,
    completed_at       TIMESTAMP,
    status             VARCHAR(20)     NOT NULL,
    documents_checked  INTEGER,
    documents_changed  INTEGER,
    failure_reason     VARCHAR(500)
);
