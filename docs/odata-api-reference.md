# OData API Reference

This document describes the SAP S/4HANA OData APIs consumed by the FI-CA CI Bridge.
All APIs are standard SAP published services available on the
[SAP Business Accelerator Hub](https://api.sap.com).

> **Locating APIs on the Hub:** Search by the API **name** listed below — not by a
> technical API ID, as identifiers are subject to change between S/4HANA releases and
> may not match what is listed in older documentation.

---

## Overview

| API Name (search term on SAP Business Hub)              | Protocol | Used For                            |
|---------------------------------------------------------|----------|-------------------------------------|
| Contract Account (FI-CA)                                | V4       | Contract account master data        |
| Contract Accounting Business Partner Invoice - Read     | V4       | Posted FI-CA accounting documents   |
| Convergent Invoicing – Billing Document                 | V2/V4    | Billing document header + items     |
| Business Partner                                        | V2       | BP data linked to contract accounts |

Use V4 where available. Fall back to V2 only if V4 is not published for the target S/4HANA release.

---

## Contract Account (FI-CA)

**Entity set:** `ContractAccount`

**Key field:** `ContractAccount` (VKONT — 12-char zero-padded)

### Fields used by this bridge

| OData Field               | SAP Internal | Type   | Notes                                          |
|---------------------------|--------------|--------|------------------------------------------------|
| `ContractAccount`         | VKONT        | String | Zero-padded; strip leading zeros in transform  |
| `ContractAccountName`     | —            | String | Trim whitespace                                |
| `BusinessPartner`         | GPART        | String | Zero-padded                                    |
| `ContractAccountCategory` | KOFIZ        | String | Account category code                          |
| `DunningProcedure`        | MAHNV        | String |                                                |
| `IncomingPaymentMethod`   | EZAWE        | String |                                                |
| `PaymentCondition`        | ZTERM        | String |                                                |

### Typical requests

```
GET /API_CA_CONTRACTACCOUNT/ContractAccount('0000200001')
GET /API_CA_CONTRACTACCOUNT/ContractAccount?$filter=BusinessPartner eq '0000100001'
```

---

## Contract Accounting Business Partner Invoice - Read

**Entity set:** `FiCADocument`

**Key fields:** `FiCADocument` (OPBEL), `FiCADocumentItem`

### Fields used by this bridge

| OData Field       | SAP Internal | Type   | Notes                                          |
|-------------------|--------------|--------|------------------------------------------------|
| `FiCADocument`    | OPBEL        | String | FI-CA document number; zero-padded             |
| `ContractAccount` | VKONT        | String | Zero-padded                                    |
| `BusinessPartner` | GPART        | String | Zero-padded                                    |
| `DocumentDate`    | BLDAT        | String | YYYYMMDD; parse with `TransformerUtils`        |
| `PostingDate`     | BUDAT        | String | YYYYMMDD                                       |
| `DueDate`         | FAEDN        | String | YYYYMMDD; 00000000 = not yet due               |
| `ClearingDate`    | AUGDT        | String | YYYYMMDD; 00000000 = not cleared               |
| `ClearingStatus`  | AUGST        | String | `OPEN`, `CLEARED`, `PARTIAL`                   |
| `Amount`          | BETRW        | String | Decimal string; FI-CA uses CR/DR sign logic    |
| `Currency`        | WAERS        | String | ISO 4217                                       |
| `ConditionType`   | KSCHL        | String | Charge category code                           |

### Typical requests

```
GET /API_FICADOCUMENT/FiCADocument?$filter=ContractAccount eq '0000200001'
GET /API_FICADOCUMENT/FiCADocument?$filter=ContractAccount eq '0000200001' and ClearingStatus eq 'OPEN'
```

---

## Convergent Invoicing – Billing Document

**Entity set:** `BillingDocument`

**Navigation property:** `to_BillingDocumentItem` (use `$expand` to include line items)

**Key field:** `BillingDocument`

### Header fields used by this bridge

| OData Field                   | SAP Internal | Type    | Notes                                      |
|-------------------------------|--------------|---------|--------------------------------------------|
| `BillingDocument`             | VBELN        | String  | Billing document number                    |
| `BillingDocumentType`         | FKART        | String  | Billing type (e.g. `ZREC`)                 |
| `BillingDocumentDate`         | FKDAT        | String  | YYYYMMDD                                   |
| `BillingDocumentIsCancelled`  | —            | Boolean |                                            |
| `CustomerNumber`              | KUNAG        | String  | Sold-to party; zero-padded                 |
| `ContractAccount`             | VKONT        | String  | Zero-padded                                |
| `NetAmount`                   | NETWR        | String  | Decimal string                             |
| `TaxAmount`                   | MWSBP        | String  | Decimal string                             |
| `TransactionCurrency`         | WAERK        | String  | ISO 4217                                   |
| `PaymentDueDate`              | ZFAELL       | String  | YYYYMMDD                                   |
| `ClearingDate`                | AUGDT        | String  | YYYYMMDD; 00000000 = not cleared           |
| `ClearingStatus`              | AUGST        | String  | `OPEN`, `CLEARED`                          |

### Line item fields (`to_BillingDocumentItem`)

| OData Field               | SAP Internal | Notes                        |
|---------------------------|--------------|------------------------------|
| `BillingDocumentItem`     | POSNR        | Item number; zero-padded     |
| `BillingDocumentItemText` | ARKTX        | Item description             |
| `Material`                | MATNR        | Material / charge code       |
| `BillingQuantity`         | FKIMG        | Decimal string               |
| `BillingQuantityUnit`     | VRKME        | Unit of measure (KWH, EA...) |
| `NetAmount`               | NETWR        | Decimal string               |
| `TaxAmount`               | MWSBP        | Decimal string               |
| `ChargingCategory`        | KSCHL        | Condition type / charge type |

### Typical requests

```
GET /API_BILLING_DOCUMENT_SRV/BillingDocument?$filter=ContractAccount eq '0000200001'&$expand=to_BillingDocumentItem
GET /API_BILLING_DOCUMENT_SRV/BillingDocument('0090001001')?$expand=to_BillingDocumentItem
```

---

## SAP Data Quirks

All SAP-specific transformations are centralised in `TransformerUtils`. The key quirks:

### Leading zeros
Numeric IDs (VKONT, GPART, OPBEL, VBELN) are zero-padded to a fixed width in SAP.
The bridge strips leading zeros before storing/returning them.
```
"0000200001"  →  "200001"
```

### Zero date
SAP uses `"00000000"` to represent a null/absent date (not yet due, not yet cleared, etc).
`TransformerUtils.parseSapDate("00000000")` returns `null`.

### Amount sign conventions
FI-CA uses receivable/payable sign conventions that may differ from simple debit/credit.
Positive amounts in FI-CA documents are typically receivables (customer owes money).
Check the specific document type (`FiCADocumentType`) before interpreting sign.

### OData envelope format
- V2 APIs wrap collection results in `{ "d": { "results": [] } }`
- V4 APIs use `{ "value": [] }`
- Single-entity V2 responses are wrapped in `{ "d": { ... } }`
- `ODataWrapper<T>` and `ODataClientBase.fetchSingle()` handle both transparently
