package com.ficabridge.service;

import com.ficabridge.model.dto.ContractAccountDTO;
import com.ficabridge.model.entity.ContractAccountEntity;
import com.ficabridge.mapper.ContractAccountMapper;
import com.ficabridge.mapper.InvoiceMapper;
import com.ficabridge.repository.ContractAccountRepository;
import com.ficabridge.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused") // fields used via Lombok constructor injection; methods are stubs
public class ContractAccountService {

    private final ContractAccountRepository contractAccountRepository;
    private final InvoiceRepository invoiceRepository;
    private final ContractAccountMapper contractAccountMapper;
    private final InvoiceMapper invoiceMapper;

    public ContractAccountDTO getByContractAccount(String contractAccount) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public List<ContractAccountDTO> getAllWithOverdueItems() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public ContractAccountEntity save(ContractAccountEntity entity) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
