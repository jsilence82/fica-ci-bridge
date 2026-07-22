package com.ficabridge.service;

import com.ficabridge.exception.ResourceNotFoundException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceMapper invoiceMapper;
    @InjectMocks private InvoiceService service;

    // ── getByBillingDocNumber ────────────────────────────────────────────────

    @Test
    void getByBillingDocNumber_found_returnsMappedDto() {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setBillingDocNumber("90001234");
        InvoiceDTO dto = new InvoiceDTO();
        dto.setBillingDocNumber("90001234");

        when(invoiceRepository.findByBillingDocNumber("90001234")).thenReturn(Optional.of(entity));
        when(invoiceMapper.toDto(entity)).thenReturn(dto);

        InvoiceDTO result = service.getByBillingDocNumber("90001234");

        assertThat(result.getBillingDocNumber()).isEqualTo("90001234");
        verify(invoiceRepository).findByBillingDocNumber("90001234");
        verify(invoiceMapper).toDto(entity);
    }

    @Test
    void getByBillingDocNumber_notFound_throwsResourceNotFoundException() {
        when(invoiceRepository.findByBillingDocNumber("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByBillingDocNumber("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    // ── getAll ───────────────────────────────────────────────────────────────

    @Test
    void getAll_returnsMappedPage_preservingTotals() {
        List<InvoiceEntity> entities = List.of(new InvoiceEntity(), new InvoiceEntity());
        Pageable pageable = PageRequest.of(0, 50);

        when(invoiceRepository.findAll(pageable)).thenReturn(new PageImpl<>(entities, pageable, 2));
        when(invoiceMapper.toDto(any(InvoiceEntity.class))).thenReturn(new InvoiceDTO());

        Page<InvoiceDTO> result = service.getAll(pageable);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(invoiceRepository).findAll(pageable);
    }

    @Test
    void getAll_emptyRepository_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 50);
        when(invoiceRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<InvoiceDTO> result = service.getAll(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // ── getByContractAccount ─────────────────────────────────────────────────

    @Test
    void getByContractAccount_returnsMatchingInvoices() {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setContractAccount("100200");
        Pageable pageable = PageRequest.of(0, 50);

        when(invoiceRepository.findByContractAccount("100200", pageable))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(invoiceMapper.toDto(any(InvoiceEntity.class))).thenReturn(new InvoiceDTO());

        assertThat(service.getByContractAccount("100200", pageable).getContent()).hasSize(1);
        verify(invoiceRepository).findByContractAccount("100200", pageable);
    }

    @Test
    void getByContractAccount_noMatches_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 50);
        when(invoiceRepository.findByContractAccount("999", pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(service.getByContractAccount("999", pageable).getContent()).isEmpty();
    }

    // ── getByStatus ──────────────────────────────────────────────────────────

    @Test
    void getByStatus_open_returnsOpenInvoices() {
        Pageable pageable = PageRequest.of(0, 50);
        when(invoiceRepository.findByStatus(InvoiceStatus.OPEN, pageable))
                .thenReturn(new PageImpl<>(List.of(new InvoiceEntity()), pageable, 1));
        when(invoiceMapper.toDto(any(InvoiceEntity.class))).thenReturn(new InvoiceDTO());

        assertThat(service.getByStatus(InvoiceStatus.OPEN, pageable).getContent()).hasSize(1);
        verify(invoiceRepository).findByStatus(InvoiceStatus.OPEN, pageable);
    }

    @Test
    void getByStatus_overdue_returnsOverdueInvoices() {
        Pageable pageable = PageRequest.of(0, 50);
        when(invoiceRepository.findByStatus(InvoiceStatus.OVERDUE, pageable))
                .thenReturn(new PageImpl<>(List.of(new InvoiceEntity(), new InvoiceEntity()), pageable, 2));
        when(invoiceMapper.toDto(any(InvoiceEntity.class))).thenReturn(new InvoiceDTO());

        assertThat(service.getByStatus(InvoiceStatus.OVERDUE, pageable).getContent()).hasSize(2);
    }

    // ── getByContractAccountAndStatus ────────────────────────────────────────

    @Test
    void getByContractAccountAndStatus_returnsFiltered() {
        Pageable pageable = PageRequest.of(0, 50);
        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE, pageable))
                .thenReturn(new PageImpl<>(List.of(new InvoiceEntity()), pageable, 1));
        when(invoiceMapper.toDto(any(InvoiceEntity.class))).thenReturn(new InvoiceDTO());

        assertThat(service.getByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE, pageable).getContent())
                .hasSize(1);
        verify(invoiceRepository).findByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE, pageable);
    }

    @Test
    void getByContractAccountAndStatus_noMatches_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 50);
        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.CLEARED, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(service.getByContractAccountAndStatus("100200", InvoiceStatus.CLEARED, pageable).getContent())
                .isEmpty();
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    void save_persistsAndReturnsEntity() {
        InvoiceEntity entity = new InvoiceEntity();
        when(invoiceRepository.save(entity)).thenReturn(entity);

        assertThat(service.save(entity)).isSameAs(entity);
        verify(invoiceRepository).save(entity);
    }
}
