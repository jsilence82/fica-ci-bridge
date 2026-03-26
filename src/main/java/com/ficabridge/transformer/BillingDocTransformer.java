package com.ficabridge.transformer;

import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.model.idoc.BillingIDoc;
import org.springframework.stereotype.Component;

/**
 * Transforms a Convergent Invoicing billing IDoc into the application's domain model.
 * This is the core of the application — all SAP field mapping, clearing status derivation,
 * OVERDUE logic, and data-quirk handling lives here.
 */
@Component
public class BillingDocTransformer {

    /**
     * Transform a parsed BillingIDoc into an InvoiceEntity ready for persistence.
     * Handles: leading zero stripping, zero-date parsing, AUGST → InvoiceStatus mapping,
     * OVERDUE derivation, whitespace trimming, and KSCHL → charge type labelling.
     */
    public InvoiceEntity transform(BillingIDoc idoc) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
