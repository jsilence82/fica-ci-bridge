CREATE TABLE invoices (
    id                 BIGSERIAL       PRIMARY KEY,
    idoc_docnum        VARCHAR(20)     NOT NULL UNIQUE,
    billing_doc_number VARCHAR(20)     NOT NULL UNIQUE,
    business_partner   VARCHAR(20)     NOT NULL,
    contract_account   VARCHAR(20)     NOT NULL,
    contract_reference VARCHAR(50),
    due_date           DATE,
    amount             NUMERIC(19, 2)  NOT NULL,
    currency           VARCHAR(5)      NOT NULL,
    status             VARCHAR(20)     NOT NULL,
    clearing_date      DATE,
    fica_doc_number    VARCHAR(20),
    created_at         TIMESTAMP       NOT NULL
);

CREATE INDEX idx_invoices_contract_account ON invoices (contract_account);
CREATE INDEX idx_invoices_status           ON invoices (status);
CREATE INDEX idx_invoices_contract_status  ON invoices (contract_account, status);
