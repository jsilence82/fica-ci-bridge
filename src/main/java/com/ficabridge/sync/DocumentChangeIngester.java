package com.ficabridge.sync;

import java.util.List;

/**
 * The only seam through which detected clearing-status changes reach the invoice cache.
 * A future event-driven source (SAP Event Mesh, IDoc/change-pointer listener) implements
 * this same interface and replaces {@link DocumentSyncScheduler} without touching
 * {@code service/} or {@code controller/}.
 */
public interface DocumentChangeIngester {

    void ingest(List<DocumentChange> changes);
}
