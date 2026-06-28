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
    void getAll_returnsMappedList() {
        List<InvoiceEntity> entities = List.of(new InvoiceEntity(), new InvoiceEntity());
        List<InvoiceDTO> dtos = List.of(new InvoiceDTO(), new InvoiceDTO());

        when(invoiceRepository.findAll()).thenReturn(entities);
        when(invoiceMapper.toDtoList(entities)).thenReturn(dtos);

        assertThat(service.getAll()).hasSize(2);
    }

    @Test
    void getAll_emptyRepository_returnsEmptyList() {
        when(invoiceRepository.findAll()).thenReturn(List.of());
        when(invoiceMapper.toDtoList(List.of())).thenReturn(List.of());

        assertThat(service.getAll()).isEmpty();
    }

    // ── getByContractAccount ─────────────────────────────────────────────────

    @Test
    void getByContractAccount_returnsMatchingInvoices() {
        InvoiceEntity entity = new InvoiceEntity();
        entity.setContractAccount("100200");
        List<InvoiceEntity> entities = List.of(entity);
        List<InvoiceDTO> dtos = List.of(new InvoiceDTO());

        when(invoiceRepository.findByContractAccount("100200")).thenReturn(entities);
        when(invoiceMapper.toDtoList(entities)).thenReturn(dtos);

        assertThat(service.getByContractAccount("100200")).hasSize(1);
        verify(invoiceRepository).findByContractAccount("100200");
    }

    @Test
    void getByContractAccount_noMatches_returnsEmptyList() {
        when(invoiceRepository.findByContractAccount("999")).thenReturn(List.of());
        when(invoiceMapper.toDtoList(List.of())).thenReturn(List.of());

        assertThat(service.getByContractAccount("999")).isEmpty();
    }

    // ── getByStatus ──────────────────────────────────────────────────────────

    @Test
    void getByStatus_open_returnsOpenInvoices() {
        List<InvoiceEntity> entities = List.of(new InvoiceEntity());
        List<InvoiceDTO> dtos = List.of(new InvoiceDTO());

        when(invoiceRepository.findByStatus(InvoiceStatus.OPEN)).thenReturn(entities);
        when(invoiceMapper.toDtoList(entities)).thenReturn(dtos);

        assertThat(service.getByStatus(InvoiceStatus.OPEN)).hasSize(1);
        verify(invoiceRepository).findByStatus(InvoiceStatus.OPEN);
    }

    @Test
    void getByStatus_overdue_returnsOverdueInvoices() {
        List<InvoiceEntity> entities = List.of(new InvoiceEntity(), new InvoiceEntity());
        List<InvoiceDTO> dtos = List.of(new InvoiceDTO(), new InvoiceDTO());

        when(invoiceRepository.findByStatus(InvoiceStatus.OVERDUE)).thenReturn(entities);
        when(invoiceMapper.toDtoList(entities)).thenReturn(dtos);

        assertThat(service.getByStatus(InvoiceStatus.OVERDUE)).hasSize(2);
    }

    // ── getByContractAccountAndStatus ────────────────────────────────────────

    @Test
    void getByContractAccountAndStatus_returnsFiltered() {
        List<InvoiceEntity> entities = List.of(new InvoiceEntity());
        List<InvoiceDTO> dtos = List.of(new InvoiceDTO());

        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE))
                .thenReturn(entities);
        when(invoiceMapper.toDtoList(entities)).thenReturn(dtos);

        assertThat(service.getByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE)).hasSize(1);
        verify(invoiceRepository).findByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE);
    }

    @Test
    void getByContractAccountAndStatus_noMatches_returnsEmptyList() {
        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.CLEARED))
                .thenReturn(List.of());
        when(invoiceMapper.toDtoList(List.of())).thenReturn(List.of());

        assertThat(service.getByContractAccountAndStatus("100200", InvoiceStatus.CLEARED)).isEmpty();
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    void save_persistsAndReturnsEntity() {
        InvoiceEntity entity = new InvoiceEntity();
        when(invoiceRepository.save(entity)).thenReturn(entity);

        assertThat(service.save(entity)).isSameAs(entity);
        verify(invoiceRepository).save(entity);
    }

    // ── existsByIdocDocnum ───────────────────────────────────────────────────

    @Test
    void existsByIdocDocnum_returnsTrue_whenExists() {
        when(invoiceRepository.existsByIdocDocnum("DOC001")).thenReturn(true);
        assertThat(service.existsByIdocDocnum("DOC001")).isTrue();
    }

    @Test
    void existsByIdocDocnum_returnsFalse_whenNotExists() {
        when(invoiceRepository.existsByIdocDocnum("NONE")).thenReturn(false);
        assertThat(service.existsByIdocDocnum("NONE")).isFalse();
    }
}
