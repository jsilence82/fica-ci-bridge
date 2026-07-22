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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenItemServiceTest {

    private static final List<InvoiceStatus> OPEN_ITEM_STATUSES =
            List.of(InvoiceStatus.OPEN, InvoiceStatus.OVERDUE);
    private static final Pageable PAGEABLE = PageRequest.of(0, 50);

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private InvoiceMapper invoiceMapper;
    @InjectMocks private OpenItemService service;

    // ── getOpenItemsByContractAccount ────────────────────────────────────────

    @Test
    void getOpenItemsByContractAccount_queriesOpenAndOverdueInSinglePagedCall() {
        when(invoiceRepository.findByContractAccountAndStatusIn("100200", OPEN_ITEM_STATUSES, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(new InvoiceEntity(), new InvoiceEntity()), PAGEABLE, 2));
        when(invoiceMapper.toDto(any(InvoiceEntity.class))).thenReturn(new InvoiceDTO());

        Page<InvoiceDTO> result = service.getOpenItemsByContractAccount("100200", PAGEABLE);

        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
        verify(invoiceRepository).findByContractAccountAndStatusIn("100200", OPEN_ITEM_STATUSES, PAGEABLE);
    }

    @Test
    void getOpenItemsByContractAccount_none_returnsEmptyPage() {
        when(invoiceRepository.findByContractAccountAndStatusIn("100200", OPEN_ITEM_STATUSES, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

        assertThat(service.getOpenItemsByContractAccount("100200", PAGEABLE).getContent()).isEmpty();
    }

    // ── getAllOpenItems ───────────────────────────────────────────────────────

    @Test
    void getAllOpenItems_queriesOpenAndOverdueAcrossAllAccounts() {
        when(invoiceRepository.findByStatusIn(OPEN_ITEM_STATUSES, PAGEABLE))
                .thenReturn(new PageImpl<>(
                        List.of(new InvoiceEntity(), new InvoiceEntity(), new InvoiceEntity()), PAGEABLE, 3));
        when(invoiceMapper.toDto(any(InvoiceEntity.class))).thenReturn(new InvoiceDTO());

        Page<InvoiceDTO> result = service.getAllOpenItems(PAGEABLE);

        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
        verify(invoiceRepository).findByStatusIn(OPEN_ITEM_STATUSES, PAGEABLE);
    }

    @Test
    void getAllOpenItems_emptyRepository_returnsEmptyPage() {
        when(invoiceRepository.findByStatusIn(OPEN_ITEM_STATUSES, PAGEABLE))
                .thenReturn(new PageImpl<>(List.of(), PAGEABLE, 0));

        assertThat(service.getAllOpenItems(PAGEABLE).getContent()).isEmpty();
    }
}
