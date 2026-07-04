ALTER TABLE invoices ADD COLUMN official_document_number VARCHAR(20);

CREATE INDEX idx_invoices_official_doc_num ON invoices (official_document_number);
