package com.ficabridge.service;

import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.mapper.InvoiceMapper;
import com.ficabridge.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@SuppressWarnings("unused") // fields used via Lombok constructor injection; methods are stubs
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;

    public InvoiceDTO getByBillingDocNumber(String billingDocNumber) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public List<InvoiceDTO> getAll() {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public List<InvoiceDTO> getByContractAccount(String contractAccount) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public List<InvoiceDTO> getByStatus(InvoiceStatus status) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public List<InvoiceDTO> getByContractAccountAndStatus(String contractAccount, InvoiceStatus status) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public InvoiceEntity save(InvoiceEntity entity) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public boolean existsByIdocDocnum(String idocDocnum) {
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
