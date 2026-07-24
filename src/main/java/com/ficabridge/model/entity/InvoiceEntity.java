package com.ficabridge.model.entity;

import com.ficabridge.model.dto.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "invoices")
public class InvoiceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String invoiceNumber;     // CAInvoicingDocument, leading zeros stripped — natural key

    @Column(nullable = false)
    private String businessPartner;      // BusinessPartner, leading zeros stripped

    @Column(nullable = false)
    private String contractAccount;      // ContractAccount, leading zeros stripped

    private String contractReference;

    /** Official Document Number (ODN) — CAOfficialDocumentNumber from API_CAINVOICINGDOCUMENT. */
    @Column(length = 20)
    private String officialDocumentNumber;

    private LocalDate dueDate;           // CANetDueDate

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;           // CAAmountInTransactionCurrency

    @Column(nullable = false, length = 5)
    private String currency;             // TransactionCurrency

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    private LocalDate clearingDate;      // derived from FI-CA doc; null when not cleared

    private String ficaDocNumber;        // CADocumentNumber from line items, leading zeros stripped

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /** Set by {@code sync.DocumentSyncScheduler} on each successful poll; null until the first sync. */
    private Instant lastSyncedAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLineItemEntity> lineItems;

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
