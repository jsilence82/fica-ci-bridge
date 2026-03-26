package com.ficabridge.model.entity;

import com.ficabridge.model.dto.InvoiceStatus;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
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
    private String idocDocnum;           // EDI_DC40.DOCNUM — used for idempotency

    @Column(unique = true, nullable = false)
    private String billingDocNumber;     // E1INVDO.VBELN, leading zeros stripped

    @Column(nullable = false)
    private String businessPartner;      // E1INVDO.GPART, leading zeros stripped

    @Column(nullable = false)
    private String contractAccount;      // E1INVDO.VKONT, leading zeros stripped

    private String contractReference;    // E1INVDO.VTREF

    private LocalDate dueDate;           // E1INVDO.FAEDN

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;           // E1INVDO.BETRW

    @Column(nullable = false, length = 5)
    private String currency;             // E1INVDO.WAERS

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;        // Derived from E1INVDO.AUGST + due date logic

    private LocalDate clearingDate;      // E1INVDO.AUGDT — null when "00000000"

    private String ficaDocNumber;        // E1INVDO.OPBEL, leading zeros stripped

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InvoiceLineItemEntity> lineItems;
}
