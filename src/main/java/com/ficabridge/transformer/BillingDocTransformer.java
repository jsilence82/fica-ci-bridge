package com.ficabridge.transformer;

import org.springframework.stereotype.Component;

/**
 * Transforms a Convergent Invoicing billing OData response into the application's domain model.
 * All SAP field mapping, clearing status derivation, OVERDUE logic, and data-quirk handling
 * lives here. Input and output types will be wired in Step 2 / Step 4.
 */
@Component
public class BillingDocTransformer {
    // transform(ODataBillingDocument) → InvoiceDTO  — implemented in Step 4
}
