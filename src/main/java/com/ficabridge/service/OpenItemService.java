package com.ficabridge.service;

import com.ficabridge.mapper.InvoiceMapper;
import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.repository.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OpenItemService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;

    public List<InvoiceDTO> getOpenItemsByContractAccount(String contractAccount) {
        List<InvoiceDTO> open = invoiceMapper.toDtoList(
                invoiceRepository.findByContractAccountAndStatus(contractAccount, InvoiceStatus.OPEN));
        List<InvoiceDTO> overdue = invoiceMapper.toDtoList(
                invoiceRepository.findByContractAccountAndStatus(contractAccount, InvoiceStatus.OVERDUE));
        List<InvoiceDTO> combined = new ArrayList<>(open);
        combined.addAll(overdue);
        return combined;
    }

    public List<InvoiceDTO> getAllOpenItems() {
        List<InvoiceDTO> open = invoiceMapper.toDtoList(
                invoiceRepository.findByStatus(InvoiceStatus.OPEN));
        List<InvoiceDTO> overdue = invoiceMapper.toDtoList(
                invoiceRepository.findByStatus(InvoiceStatus.OVERDUE));
        List<InvoiceDTO> combined = new ArrayList<>(open);
        combined.addAll(overdue);
        return combined;
    }
}
