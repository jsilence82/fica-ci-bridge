package com.ficabridge.repository;

import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.ContractAccountEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ContractAccountRepository extends JpaRepository<ContractAccountEntity, Long> {

    Optional<ContractAccountEntity> findByContractAccount(String contractAccount);

    boolean existsByContractAccount(String contractAccount);

    /**
     * Pages over the contract accounts that have at least one invoice in the given status.
     * Uses a correlated {@code exists} subquery because {@code ContractAccountEntity} and
     * {@code InvoiceEntity} share no JPA relationship — they are linked only by the
     * {@code contractAccount} string — so the filter and the page count both run in the DB
     * rather than loading every account to filter in memory.
     */
    @Query("""
            select ca from ContractAccountEntity ca
            where exists (
                select 1 from InvoiceEntity i
                where i.contractAccount = ca.contractAccount and i.status = :status
            )
            """)
    Page<ContractAccountEntity> findWithInvoiceStatus(@Param("status") InvoiceStatus status, Pageable pageable);
}
