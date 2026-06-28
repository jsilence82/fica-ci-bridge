package com.ficabridge.service;

import com.ficabridge.exception.ResourceNotFoundException;
import com.ficabridge.mapper.ContractAccountMapper;
import com.ficabridge.mapper.InvoiceMapper;
import com.ficabridge.model.dto.ContractAccountDTO;
import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.ContractAccountEntity;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.repository.ContractAccountRepository;
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
class ContractAccountServiceTest {

    @Mock private ContractAccountRepository contractAccountRepository;
    @Mock private InvoiceRepository invoiceRepository;
    @Mock private ContractAccountMapper contractAccountMapper;
    @Mock private InvoiceMapper invoiceMapper;
    @InjectMocks private ContractAccountService service;

    // ── getByContractAccount ─────────────────────────────────────────────────

    @Test
    void getByContractAccount_found_returnsDtoWithInvoices() {
        ContractAccountEntity caEntity = new ContractAccountEntity();
        caEntity.setContractAccount("100200");
        caEntity.setBusinessPartner("5678");

        ContractAccountDTO caDto = new ContractAccountDTO();
        caDto.setContractAccount("100200");

        List<InvoiceEntity> invoiceEntities = List.of(new InvoiceEntity());
        List<InvoiceDTO> invoiceDtos = List.of(new InvoiceDTO());

        when(contractAccountRepository.findByContractAccount("100200")).thenReturn(Optional.of(caEntity));
        when(contractAccountMapper.toDto(caEntity)).thenReturn(caDto);
        when(invoiceRepository.findByContractAccount("100200")).thenReturn(invoiceEntities);
        when(invoiceMapper.toDtoList(invoiceEntities)).thenReturn(invoiceDtos);

        ContractAccountDTO result = service.getByContractAccount("100200");

        assertThat(result.getContractAccount()).isEqualTo("100200");
        assertThat(result.getInvoices()).hasSize(1);
        verify(contractAccountRepository).findByContractAccount("100200");
        verify(invoiceRepository).findByContractAccount("100200");
    }

    @Test
    void getByContractAccount_found_withNoInvoices_returnsEmptyInvoiceList() {
        ContractAccountEntity caEntity = new ContractAccountEntity();
        caEntity.setContractAccount("100200");

        ContractAccountDTO caDto = new ContractAccountDTO();

        when(contractAccountRepository.findByContractAccount("100200")).thenReturn(Optional.of(caEntity));
        when(contractAccountMapper.toDto(caEntity)).thenReturn(caDto);
        when(invoiceRepository.findByContractAccount("100200")).thenReturn(List.of());
        when(invoiceMapper.toDtoList(List.of())).thenReturn(List.of());

        ContractAccountDTO result = service.getByContractAccount("100200");

        assertThat(result.getInvoices()).isEmpty();
    }

    @Test
    void getByContractAccount_notFound_throwsResourceNotFoundException() {
        when(contractAccountRepository.findByContractAccount("UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getByContractAccount("UNKNOWN"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("UNKNOWN");
    }

    // ── getAllWithOverdueItems ────────────────────────────────────────────────

    @Test
    void getAllWithOverdueItems_returnsOnlyCAsWithOverdueInvoices() {
        ContractAccountEntity ca1 = new ContractAccountEntity();
        ca1.setContractAccount("100200");
        ContractAccountEntity ca2 = new ContractAccountEntity();
        ca2.setContractAccount("100201");

        ContractAccountDTO ca1Dto = new ContractAccountDTO();
        ca1Dto.setContractAccount("100200");

        List<InvoiceEntity> overdueForCa1 = List.of(new InvoiceEntity());
        List<InvoiceDTO> overdueDtos = List.of(new InvoiceDTO());

        when(contractAccountRepository.findAll()).thenReturn(List.of(ca1, ca2));
        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE))
                .thenReturn(overdueForCa1);
        when(invoiceRepository.findByContractAccountAndStatus("100201", InvoiceStatus.OVERDUE))
                .thenReturn(List.of());
        when(contractAccountMapper.toDto(ca1)).thenReturn(ca1Dto);
        when(invoiceMapper.toDtoList(overdueForCa1)).thenReturn(overdueDtos);

        List<ContractAccountDTO> result = service.getAllWithOverdueItems();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContractAccount()).isEqualTo("100200");
        assertThat(result.get(0).getInvoices()).hasSize(1);
        verify(contractAccountMapper, never()).toDto(ca2);
    }

    @Test
    void getAllWithOverdueItems_noOverdueAnywhere_returnsEmptyList() {
        ContractAccountEntity ca = new ContractAccountEntity();
        ca.setContractAccount("100200");

        when(contractAccountRepository.findAll()).thenReturn(List.of(ca));
        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE))
                .thenReturn(List.of());

        assertThat(service.getAllWithOverdueItems()).isEmpty();
        verify(contractAccountMapper, never()).toDto(any());
    }

    @Test
    void getAllWithOverdueItems_emptyRepository_returnsEmptyList() {
        when(contractAccountRepository.findAll()).thenReturn(List.of());

        assertThat(service.getAllWithOverdueItems()).isEmpty();
    }

    @Test
    void getAllWithOverdueItems_multipleAccountsAllOverdue_returnsAll() {
        ContractAccountEntity ca1 = new ContractAccountEntity();
        ca1.setContractAccount("100200");
        ContractAccountEntity ca2 = new ContractAccountEntity();
        ca2.setContractAccount("100201");

        ContractAccountDTO ca1Dto = new ContractAccountDTO();
        ContractAccountDTO ca2Dto = new ContractAccountDTO();
        List<InvoiceEntity> overdue1 = List.of(new InvoiceEntity());
        List<InvoiceEntity> overdue2 = List.of(new InvoiceEntity(), new InvoiceEntity());
        List<InvoiceDTO> overdueDtos1 = List.of(new InvoiceDTO());
        List<InvoiceDTO> overdueDtos2 = List.of(new InvoiceDTO(), new InvoiceDTO());

        when(contractAccountRepository.findAll()).thenReturn(List.of(ca1, ca2));
        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE)).thenReturn(overdue1);
        when(invoiceRepository.findByContractAccountAndStatus("100201", InvoiceStatus.OVERDUE)).thenReturn(overdue2);
        when(contractAccountMapper.toDto(ca1)).thenReturn(ca1Dto);
        when(contractAccountMapper.toDto(ca2)).thenReturn(ca2Dto);
        when(invoiceMapper.toDtoList(overdue1)).thenReturn(overdueDtos1);
        when(invoiceMapper.toDtoList(overdue2)).thenReturn(overdueDtos2);

        List<ContractAccountDTO> result = service.getAllWithOverdueItems();

        assertThat(result).hasSize(2);
    }

    // ── save ─────────────────────────────────────────────────────────────────

    @Test
    void save_persistsAndReturnsEntity() {
        ContractAccountEntity entity = new ContractAccountEntity();
        when(contractAccountRepository.save(entity)).thenReturn(entity);

        assertThat(service.save(entity)).isSameAs(entity);
        verify(contractAccountRepository).save(entity);
    }
}
