package com.ficabridge.sync;

import com.ficabridge.exception.ResourceNotFoundException;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Applies detected clearing-status changes to the invoice cache, matching on
 * {@code billingDocNumber} so the same document ingested twice updates rather than duplicates.
 * Net-new documents are out of scope for this ingester — see {@code docs} for the sync design —
 * so a missing entity is treated as an error rather than an insert.
 */
@Component
@RequiredArgsConstructor
class JpaDocumentChangeIngester implements DocumentChangeIngester {

    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public void ingest(List<DocumentChange> changes) {
        List<InvoiceEntity> updated = new ArrayList<>();
        for (DocumentChange change : changes) {
            InvoiceEntity entity = invoiceRepository.findByBillingDocNumber(change.billingDocNumber())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Invoice not found for sync update: " + change.billingDocNumber()));
            entity.setStatus(change.newStatus());
            entity.setClearingDate(change.clearingDate());
            entity.setLastSyncedAt(Instant.now());
            updated.add(entity);
        }
        invoiceRepository.saveAll(updated);
    }
}
