CREATE TABLE contract_accounts (
    id               BIGSERIAL   PRIMARY KEY,
    contract_account VARCHAR(20) NOT NULL UNIQUE,
    business_partner VARCHAR(20) NOT NULL
);
