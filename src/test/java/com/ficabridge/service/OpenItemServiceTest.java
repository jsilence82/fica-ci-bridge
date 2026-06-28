package com.ficabridge.service;

import com.ficabridge.mapper.InvoiceMapper;
import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.repository.InvoiceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenItemServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceMapper invoiceMapper;
    @InjectMocks private OpenItemService service;

    // ── getOpenItemsByContractAccount ────────────────────────────────────────

    @Test
    void getOpenItemsByContractAccount_combinesOpenAndOverdue() {
        InvoiceEntity openEntity = new InvoiceEntity();
        InvoiceEntity overdueEntity = new InvoiceEntity();
        List<InvoiceEntity> openEntities = List.of(openEntity);
        List<InvoiceEntity> overdueEntities = List.of(overdueEntity);
        List<InvoiceDTO> openDtos = List.of(new InvoiceDTO());
        List<InvoiceDTO> overdueDtos = List.of(new InvoiceDTO());

        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.OPEN))
                .thenReturn(openEntities);
        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE))
                .thenReturn(overdueEntities);
        when(invoiceMapper.toDtoList(openEntities)).thenReturn(openDtos);
        when(invoiceMapper.toDtoList(overdueEntities)).thenReturn(overdueDtos);

        List<InvoiceDTO> result = service.getOpenItemsByContractAccount("100200");

        assertThat(result).hasSize(2);
        verify(invoiceRepository).findByContractAccountAndStatus("100200", InvoiceStatus.OPEN);
        verify(invoiceRepository).findByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE);
    }

    @Test
    void getOpenItemsByContractAccount_onlyOpen_returnsOpenOnly() {
        List<InvoiceEntity> openEntities = List.of(new InvoiceEntity(), new InvoiceEntity());
        List<InvoiceDTO> openDtos = List.of(new InvoiceDTO(), new InvoiceDTO());

        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.OPEN))
                .thenReturn(openEntities);
        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE))
                .thenReturn(List.of());
        when(invoiceMapper.toDtoList(openEntities)).thenReturn(openDtos);
        when(invoiceMapper.toDtoList(List.of())).thenReturn(List.of());

        List<InvoiceDTO> result = service.getOpenItemsByContractAccount("100200");

        assertThat(result).hasSize(2);
    }

    @Test
    void getOpenItemsByContractAccount_noneOpen_returnsEmptyList() {
        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.OPEN))
                .thenReturn(List.of());
        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE))
                .thenReturn(List.of());
        when(invoiceMapper.toDtoList(List.of())).thenReturn(List.of());

        assertThat(service.getOpenItemsByContractAccount("100200")).isEmpty();
    }

    // ── getAllOpenItems ───────────────────────────────────────────────────────

    @Test
    void getAllOpenItems_combinesOpenAndOverdueAcrossAllAccounts() {
        List<InvoiceEntity> openEntities = List.of(new InvoiceEntity(), new InvoiceEntity());
        List<InvoiceEntity> overdueEntities = List.of(new InvoiceEntity());
        List<InvoiceDTO> openDtos = List.of(new InvoiceDTO(), new InvoiceDTO());
        List<InvoiceDTO> overdueDtos = List.of(new InvoiceDTO());

        when(invoiceRepository.findByStatus(InvoiceStatus.OPEN)).thenReturn(openEntities);
        when(invoiceRepository.findByStatus(InvoiceStatus.OVERDUE)).thenReturn(overdueEntities);
        when(invoiceMapper.toDtoList(openEntities)).thenReturn(openDtos);
        when(invoiceMapper.toDtoList(overdueEntities)).thenReturn(overdueDtos);

        List<InvoiceDTO> result = service.getAllOpenItems();

        assertThat(result).hasSize(3);
        verify(invoiceRepository).findByStatus(InvoiceStatus.OPEN);
        verify(invoiceRepository).findByStatus(InvoiceStatus.OVERDUE);
    }

    @Test
    void getAllOpenItems_emptyRepository_returnsEmptyList() {
        when(invoiceRepository.findByStatus(InvoiceStatus.OPEN)).thenReturn(List.of());
        when(invoiceRepository.findByStatus(InvoiceStatus.OVERDUE)).thenReturn(List.of());
        when(invoiceMapper.toDtoList(List.of())).thenReturn(List.of());

        assertThat(service.getAllOpenItems()).isEmpty();
    }

    @Test
    void getAllOpenItems_onlyOverdue_returnsOverdueOnly() {
        List<InvoiceEntity> overdueEntities = List.of(new InvoiceEntity());
        List<InvoiceDTO> overdueDtos = List.of(new InvoiceDTO());

        when(invoiceRepository.findByStatus(InvoiceStatus.OPEN)).thenReturn(List.of());
        when(invoiceRepository.findByStatus(InvoiceStatus.OVERDUE)).thenReturn(overdueEntities);
        when(invoiceMapper.toDtoList(List.of())).thenReturn(List.of());
        when(invoiceMapper.toDtoList(overdueEntities)).thenReturn(overdueDtos);

        List<InvoiceDTO> result = service.getAllOpenItems();

        assertThat(result).hasSize(1);
    }
}
