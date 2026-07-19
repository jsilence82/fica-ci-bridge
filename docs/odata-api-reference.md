# OData API Reference

This document describes the SAP S/4HANA OData APIs consumed by the FI-CA CI Bridge.
All APIs are standard SAP published services available on the
[SAP Business Accelerator Hub](https://api.sap.com).

> **Locating APIs on the Hub:** Search by the **API ID** (e.g. `API_CAINVOICINGDOCUMENT`)
> or by the API name. Identifiers can vary between S/4HANA releases — always verify the
> entity set names in the service metadata (`/$metadata`) on your target system.

---

## Overview

| API Name                                              | API ID                    | Protocol | Client class                  | Status        |
|-------------------------------------------------------|---------------------------|----------|-------------------------------|---------------|
| Contract Account (FI-CA)                              | API_CA_CONTRACTACCOUNT    | V2       | `ContractAccountClient`       | Implemented   |
| FI-CA Document                                        | API_FICADOCUMENT          | V2       | `FicaDocumentClient`          | Implemented   |
| CA Invoicing Document - Read                          | API_CAINVOICINGDOCUMENT   | **V4**   | `BillingDocumentClient`       | Implemented   |
| Business Partner                                      | API_BUSINESS_PARTNER      | V2       | —                             | Not implemented |
| Contract Accounting Business Partner Invoice - Read   | API_CABUSPARTINVOICE      | **V4**   | —                             | Not implemented |

> **Do not use `API_BILLING_DOCUMENT_SRV`** — that is the SD (Sales & Distribution) billing API
> (OData V2, entity `BillingDocument`). It has a completely different entity structure and is
> unrelated to FI-CA / Convergent Invoicing.

---

## API_CA_CONTRACTACCOUNT — Contract Account (FI-CA)

**Protocol:** OData V2

**Entity set:** `ContractAccount`

**Key field:** `ContractAccount` (VKONT — 12-char, zero-padded)

**Service path:** `/API_CA_CONTRACTACCOUNT/ContractAccount`

### Fields used by this bridge

| OData Field               | SAP Internal | Type   | Notes                                          |
|---------------------------|--------------|--------|------------------------------------------------|
| `ContractAccount`         | VKONT        | String | Zero-padded to 12 chars; strip in transformer  |
| `ContractAccountName`     | —            | String | Trim whitespace                                |
| `BusinessPartner`         | GPART        | String | Zero-padded                                    |
| `ContractAccountCategory` | KOFIZ        | String | Account category code                          |
| `AccountDeterminationCode`| KOFIZ        | String | Account determination ID                       |
| `DunningProcedure`        | MAHNV        | String |                                                |
| `IncomingPaymentMethod`   | EZAWE        | String |                                                |
| `OutgoingPaymentMethod`   | —            | String |                                                |
| `PaymentCondition`        | ZTERM        | String |                                                |
| `ClearingCategory`        | —            | String |                                                |

### Typical requests

```
GET /API_CA_CONTRACTACCOUNT/ContractAccount('0000200001')
GET /API_CA_CONTRACTACCOUNT/ContractAccount?$filter=BusinessPartner eq '0000100001'
```

---

## API_FICADOCUMENT — FI-CA Document

**Protocol:** OData V2

**Entity set:** `FiCADocument`

**Key fields:** `FiCADocument` (OPBEL), `FiCADocumentItem`

**Service path:** `/API_FICADOCUMENT/FiCADocument`

Used by this bridge to query **posted FI-CA accounting documents** and **open receivable items**.
These are the raw ledger postings in FI-CA — one document per billing/clearing event. Also the
source API for `DocumentSyncScheduler` (`sync/`) — see [docs/architecture.md](architecture.md#document-sync-sync)
and the README's [Document Sync](../README.md#document-sync) section.

> **Note:** Dates in this API are returned as YYYYMMDD strings and must be parsed with
> `TransformerUtils.parseSapDate()`. This differs from `API_CAINVOICINGDOCUMENT` (V4),
> which returns ISO `yyyy-MM-dd` dates that Jackson deserialises directly into `LocalDate`.

### Fields used by this bridge

| OData Field        | SAP Internal | Type   | Notes                                          |
|--------------------|--------------|--------|------------------------------------------------|
| `FiCADocument`     | OPBEL        | String | FI-CA document number; zero-padded             |
| `FiCADocumentItem` | OPUPW        | String | Item number within the document                |
| `FiCADocumentType` | BLART        | String | Document type code                             |
| `ContractAccount`  | VKONT        | String | Zero-padded                                    |
| `BusinessPartner`  | GPART        | String | Zero-padded                                    |
| `DocumentDate`     | BLDAT        | String | YYYYMMDD — parse with `TransformerUtils`       |
| `PostingDate`      | BUDAT        | String | YYYYMMDD                                       |
| `DueDate`          | FAEDN        | String | YYYYMMDD; `"00000000"` = not yet due           |
| `ClearingDate`     | AUGDT        | String | YYYYMMDD; `"00000000"` = not cleared           |
| `ClearingStatus`   | AUGST        | String | `OPEN`, `CLEARED`, `PARTIAL`                   |
| `Amount`           | BETRW        | String | Decimal string; FI-CA CR/DR sign conventions   |
| `Currency`         | WAERS        | String | ISO 4217                                       |
| `ConditionType`    | KSCHL        | String | Charge category / condition type code          |

### Typical requests

```
GET /API_FICADOCUMENT/FiCADocument?$filter=ContractAccount eq '0000200001'
GET /API_FICADOCUMENT/FiCADocument?$filter=ContractAccount eq '0000200001' and ClearingStatus eq 'OPEN'
GET /API_FICADOCUMENT/FiCADocument('0000001234')
```

---

## API_CAINVOICINGDOCUMENT — CA Invoicing Document Read

**Protocol:** OData **V4**

**Entity set (header):** `CAInvcgDocument`

**Entity set (items):** `CAInvcgDocItem`

**Key field (header):** `CAInvoicingDocument` (max 12 chars)

**Key fields (items):** `CAInvoicingDocument` + `CAInvcgDocItem`

**Navigation property:** `_CAInvcgDocItem` — expand to include line items in the header response

**Full service path:**
```
/sap/opu/odata4/sap/api_cainvoicingdocument/srvd_a2x/sap/cainvoicingdocument/0001
```

This is the primary invoicing API for FI-CA Convergent Invoicing. It exposes invoice headers
and line items as they appear in the Convergent Invoicing module, distinct from the raw FI-CA
accounting documents in `API_FICADOCUMENT`.

> **Date format:** This V4 API returns dates as ISO strings (`yyyy-MM-dd`). Jackson with
> `JavaTimeModule` deserialises these directly into `LocalDate` — `TransformerUtils.parseSapDate()`
> is **not** used for this API's date fields.

> **Amount format:** Amounts (`Edm.Decimal`) may be returned as either a JSON number or a
> quoted string depending on the SAP release. Model fields are typed as `String` to handle both.

### Header fields — `CAInvcgDocument`

| OData Field                       | Type      | Notes                                                               |
|-----------------------------------|-----------|---------------------------------------------------------------------|
| `CAInvoicingDocument`             | String    | Invoicing document number; strip leading zeros in transformer       |
| `CAInvcgDocumentType`             | String    | Document type (e.g. `ZREC`)                                        |
| `DocumentDate`                    | LocalDate | ISO date; creation date of the document                            |
| `CAPostingDate`                   | LocalDate | ISO date; FI-CA posting date                                       |
| `CANetDueDate`                    | LocalDate | ISO date; net payment due date                                     |
| `BusinessPartner`                 | String    | Zero-padded; strip in transformer                                   |
| `ContractAccount`                 | String    | Zero-padded; strip in transformer                                   |
| `CAContract`                      | String    | Contract reference                                                  |
| `CompanyCode`                     | String    | SAP company code                                                    |
| `CAAmountInTransactionCurrency`   | String    | Total amount; signed per FI-CA convention                          |
| `TransactionCurrency`             | String    | ISO 4217 currency code                                              |
| `CAOfficialDocumentNumber`        | String    | ODN — externally visible reference number (max 16); trim whitespace |
| `CAInvcgReversalDocument`         | String    | Non-blank = this document has been reversed by another doc          |
| `CAInvcgReversedDocument`         | String    | Non-blank = this document itself reverses another doc               |
| `CAInvcgIsDocumentPosted`         | Boolean   | `true` when the document is fully posted in FI-CA                  |
| `CAInvcgIsDocumentPreliminary`    | Boolean   | `true` for preliminary/pro-forma invoices                          |
| `CAInvcgCreationDate`             | LocalDate | ISO date; when the invoicing document was created                  |
| `CAInvcgDocPeriodStartDate`       | LocalDate | ISO date; start of the billing period                              |

### Item fields — `CAInvcgDocItem`

| OData Field                       | Type      | Notes                                                               |
|-----------------------------------|-----------|---------------------------------------------------------------------|
| `CAInvoicingDocument`             | String    | Parent document key                                                 |
| `CAInvcgDocItem`                  | String    | Sequential item number (max 8 chars; e.g. `"00000001"`)            |
| `CAMainTransaction`               | String    | Main transaction code — primary charge categorisation              |
| `CASubTransaction`                | String    | Sub-transaction code — secondary categorisation                    |
| `CAConditionType`                 | String    | Pricing condition code; maps to `chargingCategory` in the DTO      |
| `TransactionCurrency`             | String    | ISO 4217                                                            |
| `CAAmountInTransactionCurrency`   | String    | Item amount; signed per FI-CA convention                           |
| `CATaxAmountInTransCurrency`      | String    | Tax amount for this item                                            |
| `CATaxBaseAmount`                 | String    | Taxable base amount                                                 |
| `CATaxRateInPercent`              | String    | Tax rate applied                                                    |
| `TaxCode`                         | String    | SAP tax code                                                        |
| `CATaxIsIncluded`                 | Boolean   | `true` when amount is gross (tax included)                          |
| `Quantity`                        | String    | Billed quantity (decimal string)                                    |
| `UnitOfMeasure`                   | String    | SAP internal UoM code (e.g. `KWH`, `EA`)                           |
| `UnitOfMeasureISOCode`            | String    | ISO UoM code                                                        |
| `CANetDueDate`                    | LocalDate | ISO date; item-level due date                                       |
| `CAItemPeriodStartDate`           | LocalDate | ISO date; start of the service/billing period for this item         |
| `CAItemPeriodEndDate`             | LocalDate | ISO date; end of the service/billing period for this item           |
| `CADocumentNumber`                | String    | Linked FI-CA accounting document number (OPBEL)                    |
| `CAClearingDocumentNumber`        | String    | Non-blank = this item has been cleared; used for status derivation  |
| `CAClearingAmountInTransCrcy`     | String    | Amount that was cleared against this item                           |
| `CAInvcgDocItemIsReversal`        | Boolean   | `true` when this item is itself a reversal line                     |

### Status derivation

Invoice status is derived from item-level clearing data (no explicit status field on this API):

| Condition | Derived status |
|---|---|
| `CAInvcgReversalDocument` non-blank | `REVERSED` |
| All items have non-blank `CAClearingDocumentNumber` | `CLEARED` |
| Some (not all) items have non-blank `CAClearingDocumentNumber` | `PARTIALLY_PAID` |
| No items cleared + `CANetDueDate` is in the past | `OVERDUE` |
| No items cleared + due date is future or null | `OPEN` |

### Typical requests

```
# All invoices for a contract account (with items expanded)
GET /sap/opu/odata4/sap/api_cainvoicingdocument/srvd_a2x/sap/cainvoicingdocument/0001
    /CAInvcgDocument?$filter=ContractAccount eq '0000200001'&$expand=_CAInvcgDocItem

# Single invoice by document number (named key notation required for OData V4)
GET /sap/opu/odata4/sap/api_cainvoicingdocument/srvd_a2x/sap/cainvoicingdocument/0001
    /CAInvcgDocument(CAInvoicingDocument='000090001001')?$expand=_CAInvcgDocItem
```

---

## API_BUSINESS_PARTNER — Business Partner *(not implemented)*

**Protocol:** OData V2

Would provide business partner master data (name, address, contact) linked to contract accounts.
Not currently implemented — `ContractAccountDTO` does not surface BP details beyond the BP number.

---

## API_CABUSPARTINVOICE — Contract Accounting Business Partner Invoice - Read *(not implemented)*

**Protocol:** OData **V4**

**Primary entity:** `CABPInvcEnhcdForDspCrcy` (parameterised by display currency)

**Key field:** `CABusPartnerInvoiceUUID` (UUID — not a readable document number)

This API provides a customer-facing view of invoices, complementary to `API_CAINVOICINGDOCUMENT`.
It is not currently implemented but would add:

| Field | Value |
|---|---|
| `CABusPartnerInvoiceStatus` | Explicit invoice status (vs. our current derivation from item clearing) |
| `CAClearingDate` per item | Item-level clearing date (currently always `null` in `InvoiceDTO`) |
| `_BusPartInvoiceCorrespncEnhcd` | Correspondence / printed invoice PDF via `CACorrespondenceBinary` |

**Complication:** The entity set is parameterised — a display currency must be supplied in the
URL path (`/CABPInvcEnhcdForDspCrcy/{P_DisplayCurrency}/Set`), and the primary key is a UUID,
making it harder to correlate back to the invoicing document number from `API_CAINVOICINGDOCUMENT`.
A new `BusPartnerInvoiceClient` would be required.

---

## SAP Data Quirks

All SAP-specific transformations are centralised in `TransformerUtils`.

### Leading zeros
Numeric IDs (VKONT, GPART, OPBEL) are zero-padded to a fixed width in SAP.
The bridge strips leading zeros before storing or returning them.
```
"0000200001"  →  "200001"
```

### Date formats
Two different date representations are in use across APIs:

| API | Format | Example | Handling |
|---|---|---|---|
| `API_FICADOCUMENT` (V2) | YYYYMMDD string | `"20240415"` | `TransformerUtils.parseSapDate()` |
| `API_CA_CONTRACTACCOUNT` (V2) | YYYYMMDD string | `"20240415"` | `TransformerUtils.parseSapDate()` |
| `API_CAINVOICINGDOCUMENT` (V4) | ISO string | `"2024-04-15"` | Jackson `JavaTimeModule` → `LocalDate` directly |

`parseSapDate("00000000")` and `parseSapDate(null)` both return `null`.

### Amount sign conventions
FI-CA uses receivable/payable sign conventions. Positive amounts in FI-CA documents typically
represent receivables (the customer owes the utility money). Always verify sign interpretation
against the specific document type and business context.

Amounts from `API_CAINVOICINGDOCUMENT` are typed as `String` in the model because SAP OData V4
implementations may return `Edm.Decimal` as either a JSON number or a quoted string. Use
`TransformerUtils.parseSapAmount()` to convert to `BigDecimal`.

### OData envelope format
- **V2 collections:** `{ "d": { "results": [ … ] } }`
- **V2 single entity:** `{ "d": { … } }`
- **V4 collections:** `{ "value": [ … ] }`
- **V4 single entity:** the entity object directly

`ODataWrapper<T>` and `ODataClientBase` handle both transparently. Check the API's `$metadata`
to confirm which version a given entity set uses before adding a new client.

### OData V4 key notation
OData V4 requires **named key** notation for single-entity lookups when the key field name
is not implied:
```
# V4 — named key (required by API_CAINVOICINGDOCUMENT)
CAInvcgDocument(CAInvoicingDocument='000090001001')

# V2 — positional key (used by API_FICADOCUMENT and API_CA_CONTRACTACCOUNT)
FiCADocument('0000001234')
```
