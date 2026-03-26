package com.ficabridge.repository;

import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Long> {

    Optional<InvoiceEntity> findByBillingDocNumber(String billingDocNumber);

    Optional<InvoiceEntity> findByIdocDocnum(String idocDocnum);

    List<InvoiceEntity> findByContractAccount(String contractAccount);

    List<InvoiceEntity> findByStatus(InvoiceStatus status);

    List<InvoiceEntity> findByContractAccountAndStatus(String contractAccount, InvoiceStatus status);

    boolean existsByIdocDocnum(String idocDocnum);

    boolean existsByBillingDocNumber(String billingDocNumber);
}
