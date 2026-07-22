package com.ficabridge.repository;

import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.InvoiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

    Optional<InvoiceEntity> findByBillingDocNumber(String billingDocNumber);

    List<InvoiceEntity> findByContractAccount(String contractAccount);

    List<InvoiceEntity> findByStatus(InvoiceStatus status);

    List<InvoiceEntity> findByStatusIn(List<InvoiceStatus> statuses);

    List<InvoiceEntity> findByContractAccountAndStatus(String contractAccount, InvoiceStatus status);

    // Paged overloads for the /api/invoices listing endpoint. The unpaged List variants above are
    // still used by ContractAccountService and OpenItemService, which need the full set in-memory;
    // findAll(Pageable) is inherited from JpaRepository.
    Page<InvoiceEntity> findByContractAccount(String contractAccount, Pageable pageable);

    Page<InvoiceEntity> findByStatus(InvoiceStatus status, Pageable pageable);

    Page<InvoiceEntity> findByContractAccountAndStatus(String contractAccount, InvoiceStatus status, Pageable pageable);

    // Paged multi-status lookups backing the /api/payments open-item endpoint (OPEN + OVERDUE in a
    // single DB query, rather than two list queries merged in memory).
    Page<InvoiceEntity> findByStatusIn(List<InvoiceStatus> statuses, Pageable pageable);

    Page<InvoiceEntity> findByContractAccountAndStatusIn(String contractAccount, List<InvoiceStatus> statuses, Pageable pageable);

    boolean existsByBillingDocNumber(String billingDocNumber);
}
