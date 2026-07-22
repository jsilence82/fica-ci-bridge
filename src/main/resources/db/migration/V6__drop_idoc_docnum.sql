-- Drop the IDoc-era deduplication column. This project sources data via OData, not IDoc, so
-- idoc_docnum never had a natural value; billing_doc_number (also NOT NULL UNIQUE) is the natural
-- key used for idempotency everywhere in the codebase. The UNIQUE constraint on the column is
-- dropped with it.
ALTER TABLE invoices DROP COLUMN idoc_docnum;
