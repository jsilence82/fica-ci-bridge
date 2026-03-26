CREATE TABLE invoice_line_items (
    id                    BIGSERIAL      PRIMARY KEY,
    invoice_id            BIGINT         NOT NULL REFERENCES invoices (id),
    line_item_number      VARCHAR(10)    NOT NULL,
    condition_type        VARCHAR(10)    NOT NULL,
    charge_type           VARCHAR(50),
    tax_code              VARCHAR(10),
    tax_base_amount       NUMERIC(19, 2),
    tax_jurisdiction_code VARCHAR(20),
    amount                NUMERIC(19, 2) NOT NULL
);

CREATE INDEX idx_line_items_invoice_id ON invoice_line_items (invoice_id);
