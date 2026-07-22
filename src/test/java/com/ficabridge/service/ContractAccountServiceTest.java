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
    void getAllWithOverdueItems_mapsPageAndPopulatesOverdueInvoices() {
        // The repository query already restricts to accounts with an overdue invoice.
        ContractAccountEntity ca1 = new ContractAccountEntity();
        ca1.setContractAccount("100200");
        ContractAccountDTO ca1Dto = new ContractAccountDTO();
        ca1Dto.setContractAccount("100200");
        List<InvoiceEntity> overdueForCa1 = List.of(new InvoiceEntity());
        List<InvoiceDTO> overdueDtos = List.of(new InvoiceDTO());
        Pageable pageable = PageRequest.of(0, 50);

        when(contractAccountRepository.findWithInvoiceStatus(InvoiceStatus.OVERDUE, pageable))
                .thenReturn(new PageImpl<>(List.of(ca1), pageable, 1));
        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE))
                .thenReturn(overdueForCa1);
        when(contractAccountMapper.toDto(ca1)).thenReturn(ca1Dto);
        when(invoiceMapper.toDtoList(overdueForCa1)).thenReturn(overdueDtos);

        Page<ContractAccountDTO> result = service.getAllWithOverdueItems(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().get(0).getContractAccount()).isEqualTo("100200");
        assertThat(result.getContent().get(0).getInvoices()).hasSize(1);
    }

    @Test
    void getAllWithOverdueItems_emptyPage_returnsEmptyWithoutMapping() {
        Pageable pageable = PageRequest.of(0, 50);
        when(contractAccountRepository.findWithInvoiceStatus(InvoiceStatus.OVERDUE, pageable))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(service.getAllWithOverdueItems(pageable).getContent()).isEmpty();
        verify(contractAccountMapper, never()).toDto(any());
    }

    @Test
    void getAllWithOverdueItems_multipleAccounts_mapsAllWithTheirOverdueInvoices() {
        ContractAccountEntity ca1 = new ContractAccountEntity();
        ca1.setContractAccount("100200");
        ContractAccountEntity ca2 = new ContractAccountEntity();
        ca2.setContractAccount("100201");
        Pageable pageable = PageRequest.of(0, 50);

        when(contractAccountRepository.findWithInvoiceStatus(InvoiceStatus.OVERDUE, pageable))
                .thenReturn(new PageImpl<>(List.of(ca1, ca2), pageable, 2));
        when(invoiceRepository.findByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE))
                .thenReturn(List.of(new InvoiceEntity()));
        when(invoiceRepository.findByContractAccountAndStatus("100201", InvoiceStatus.OVERDUE))
                .thenReturn(List.of(new InvoiceEntity(), new InvoiceEntity()));
        when(contractAccountMapper.toDto(ca1)).thenReturn(new ContractAccountDTO());
        when(contractAccountMapper.toDto(ca2)).thenReturn(new ContractAccountDTO());
        when(invoiceMapper.toDtoList(any())).thenReturn(List.of(new InvoiceDTO()));

        Page<ContractAccountDTO> result = service.getAllWithOverdueItems(pageable);

        assertThat(result.getContent()).hasSize(2);
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
