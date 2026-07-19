package com.ficabridge.sync;

import com.ficabridge.client.FicaDocumentClient;
import com.ficabridge.exception.ODataClientException;
import com.ficabridge.model.dto.FicaDocumentDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.model.entity.SyncRunEntity;
import com.ficabridge.model.odata.ODataFicaDocument;
import com.ficabridge.repository.InvoiceRepository;
import com.ficabridge.repository.SyncRunRepository;
import com.ficabridge.transformer.FiCaDocTransformer;
import com.ficabridge.transformer.TransformerUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Polls SAP for clearing-status transitions on cached OPEN/OVERDUE invoices and hands
 * detected changes to a {@link DocumentChangeIngester}. Scoped to status transitions only —
 * discovery of net-new invoices is a separate, later piece of work.
 *
 * <p><b>Known limitations:</b>
 * <ul>
 *   <li><b>Single-instance only.</b> If this app scales to multiple replicas, {@code @Scheduled}
 *       runs on every replica and multiplies SAP call volume for no benefit. Before running with
 *       more than one replica, either move this to a Kubernetes {@code CronJob} or add a
 *       distributed lock (e.g. ShedLock) so only one replica executes per cycle.</li>
 *   <li><b>Demo-scale mechanism.</b> Polling is not the production answer at real volume (tens of
 *       thousands of documents/month). The production path is event-driven ingestion (SAP Event
 *       Mesh, or an IDoc/change-pointer trigger on document clearing) implementing the same
 *       {@link DocumentChangeIngester}, replacing this scheduler entirely.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentSyncScheduler {

    private final InvoiceRepository invoiceRepository;
    private final SyncRunRepository syncRunRepository;
    private final FicaDocumentClient ficaDocumentClient;
    private final DocumentChangeIngester ingester;
    private final FiCaDocTransformer ficaDocTransformer;

    @Scheduled(initialDelayString = "${sync.initial-delay:PT1H}", fixedDelayString = "${sync.poll-interval:PT1H}")
    void syncOpenDocuments() {
        SyncRunEntity run = SyncRunEntity.started();

        List<InvoiceEntity> openDocs = invoiceRepository.findByStatusIn(
                List.of(InvoiceStatus.OPEN, InvoiceStatus.OVERDUE));

        Map<String, List<InvoiceEntity>> byContractAccount = openDocs.stream()
                .collect(Collectors.groupingBy(InvoiceEntity::getContractAccount));

        List<DocumentChange> changes = new ArrayList<>();
        String failureReason = null;

        for (Map.Entry<String, List<InvoiceEntity>> entry : byContractAccount.entrySet()) {
            try {
                // one call per contract account, not one per document. Deliberately unfiltered by
                // clearing status — findOpenItemsByContractAccount's server-side "still open" filter
                // would hide exactly the documents that transitioned out of OPEN/OVERDUE, which is
                // what this job exists to detect.
                List<ODataFicaDocument> current = ficaDocumentClient.findByContractAccount(entry.getKey());
                changes.addAll(diff(entry.getValue(), current));
            } catch (ODataClientException e) {
                // one failed contract account must not poison the rest of the batch or the next run
                failureReason = e.getMessage();
                log.warn("Document sync failed for contract account {}: {}", entry.getKey(), e.getMessage());
            }
        }

        ingester.ingest(changes);

        if (failureReason != null) {
            run.failed(failureReason);
        } else {
            run.completed(openDocs.size(), changes.size());
        }
        syncRunRepository.save(run);
    }

    /**
     * Compares cached clearing status/date against current SAP values. Matched by the linked
     * FI-CA document number ({@code InvoiceEntity.ficaDocNumber}), not the CI invoicing document
     * number this bridge otherwise keys invoices by — {@code API_FICADOCUMENT} and
     * {@code API_CAINVOICINGDOCUMENT} are different SAP objects. Cached entities with no linked
     * FI-CA document yet are skipped. Emits a change only for entries whose derived status or
     * clearing date actually differs from what's cached.
     */
    List<DocumentChange> diff(List<InvoiceEntity> cached, List<ODataFicaDocument> current) {
        Map<String, InvoiceEntity> cachedByFicaDocNumber = cached.stream()
                .filter(e -> e.getFicaDocNumber() != null)
                .collect(Collectors.toMap(InvoiceEntity::getFicaDocNumber, e -> e, (a, b) -> a));

        List<DocumentChange> changes = new ArrayList<>();
        for (ODataFicaDocument doc : current) {
            String ficaDocNumber = TransformerUtils.stripLeadingZeros(doc.getFicaDocument());
            InvoiceEntity entity = cachedByFicaDocNumber.get(ficaDocNumber);
            if (entity == null) {
                continue; // not one of our cached open/overdue docs, or discovery (out of scope)
            }

            FicaDocumentDTO transformed = ficaDocTransformer.transform(doc);
            boolean statusChanged = transformed.getStatus() != entity.getStatus();
            boolean clearingDateChanged = !Objects.equals(transformed.getClearingDate(), entity.getClearingDate());
            if (statusChanged || clearingDateChanged) {
                changes.add(new DocumentChange(
                        entity.getBillingDocNumber(),
                        entity.getContractAccount(),
                        transformed.getStatus(),
                        transformed.getClearingDate()));
            }
        }
        return changes;
    }
}
