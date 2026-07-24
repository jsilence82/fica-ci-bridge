package com.ficabridge.sync;

import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Applies detected clearing-status changes to the invoice cache, matching on
 * {@code invoiceNumber} so the same document ingested twice updates rather than duplicates.
 * <p>
 * Net-new documents are out of scope for this ingester (discovery sync is separate work), so a
 * change whose invoice isn't in the cache is <b>skipped and logged</b> — deliberately not an insert,
 * and deliberately not a throw. Throwing would roll back the whole {@code @Transactional} batch, so
 * a single unknown document would discard every legitimate transition in the same call. That matters
 * because this seam also serves future event-driven sources, which will legitimately deliver
 * documents the cache has never seen; one such event must not poison the rest of the batch.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class JpaDocumentChangeIngester implements DocumentChangeIngester {

    private final InvoiceRepository invoiceRepository;

    @Override
    @Transactional
    public void ingest(List<DocumentChange> changes) {
        List<InvoiceEntity> updated = new ArrayList<>();
        int skipped = 0;
        for (DocumentChange change : changes) {
            Optional<InvoiceEntity> found = invoiceRepository.findByInvoiceNumber(change.invoiceNumber());
            if (found.isEmpty()) {
                skipped++;
                continue;
            }
            InvoiceEntity entity = found.get();
            entity.setStatus(change.newStatus());
            entity.setClearingDate(change.clearingDate());
            entity.setLastSyncedAt(Instant.now());
            updated.add(entity);
        }
        invoiceRepository.saveAll(updated);
        if (skipped > 0) {
            log.warn("Document sync ingest skipped {} change(s) for invoices not in the cache "
                    + "(net-new — out of scope for status-transition sync)", skipped);
        }
    }
}
