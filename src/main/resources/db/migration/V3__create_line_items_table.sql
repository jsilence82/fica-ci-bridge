CREATE TABLE invoice_line_items (
    id                BIGSERIAL       PRIMARY KEY,
    invoice_id        BIGINT          NOT NULL REFERENCES invoices (id),
    item_number       VARCHAR(10)     NOT NULL,
    description       VARCHAR(200),
    material          VARCHAR(40),
    quantity          NUMERIC(19, 3),
    quantity_unit     VARCHAR(10),
    net_amount        NUMERIC(19, 2)  NOT NULL,
    tax_amount        NUMERIC(19, 2),
    currency          VARCHAR(5),
    charging_category VARCHAR(10)
);

CREATE INDEX idx_line_items_invoice_id ON invoice_line_items (invoice_id);
