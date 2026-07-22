package com.ficabridge.service;

import com.ficabridge.exception.ResourceNotFoundException;
import com.ficabridge.mapper.ContractAccountMapper;
import com.ficabridge.mapper.InvoiceMapper;
import com.ficabridge.model.dto.ContractAccountDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.ContractAccountEntity;
import com.ficabridge.repository.ContractAccountRepository;
import com.ficabridge.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ContractAccountService {

    private final ContractAccountRepository contractAccountRepository;
    private final InvoiceRepository invoiceRepository;
    private final ContractAccountMapper contractAccountMapper;
    private final InvoiceMapper invoiceMapper;

    public ContractAccountDTO getByContractAccount(String contractAccount) {
        ContractAccountEntity entity = contractAccountRepository.findByContractAccount(contractAccount)
                .orElseThrow(() -> new ResourceNotFoundException("ContractAccount not found: " + contractAccount));
        ContractAccountDTO dto = contractAccountMapper.toDto(entity);
        dto.setInvoices(invoiceMapper.toDtoList(invoiceRepository.findByContractAccount(contractAccount)));
        return dto;
    }

    public Page<ContractAccountDTO> getAllWithOverdueItems(Pageable pageable) {
        // The DB query already restricts to accounts that have an overdue invoice, so only the
        // page's accounts remain — populate each with its overdue invoices.
        return contractAccountRepository.findWithInvoiceStatus(InvoiceStatus.OVERDUE, pageable)
                .map(ca -> {
                    ContractAccountDTO dto = contractAccountMapper.toDto(ca);
                    dto.setInvoices(invoiceMapper.toDtoList(invoiceRepository.findByContractAccountAndStatus(
                            ca.getContractAccount(), InvoiceStatus.OVERDUE)));
                    return dto;
                });
    }

    public ContractAccountEntity save(ContractAccountEntity entity) {
        return contractAccountRepository.save(entity);
    }
}
