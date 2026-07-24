-- Correct SAP terminology: the cache key is SAP's CAInvoicingDocument (FI-CA Convergent
-- Invoicing), an *invoice* document — not an SD "billing document". Rename the column to match
-- the domain field (InvoiceEntity.invoiceNumber). The UNIQUE constraint follows the column.
ALTER TABLE invoices RENAME COLUMN billing_doc_number TO invoice_number;
