package com.ficabridge.service;

import com.ficabridge.model.dto.AleAcknowledgementDTO;
import com.ficabridge.model.idoc.BillingIDoc;
import com.ficabridge.transformer.BillingDocTransformer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates inbound IDoc processing: idempotency check, transformation, persistence,
 * and ALE acknowledgement construction.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@SuppressWarnings("unused") // fields used via Lombok constructor injection; methods are stubs
public class IdocProcessingService {

    private final BillingDocTransformer billingDocTransformer;
    private final InvoiceService invoiceService;

    /**
     * Process an inbound CI billing IDoc.
     * Performs idempotency check on DOCNUM, delegates transformation to
     * BillingDocTransformer, persists via InvoiceService, and returns an
     * ALE acknowledgement. On duplicate, logs a warning and returns ack
     * for the existing document without reprocessing.
     */
    public AleAcknowledgementDTO processBillingIDoc(BillingIDoc billingIDoc) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
