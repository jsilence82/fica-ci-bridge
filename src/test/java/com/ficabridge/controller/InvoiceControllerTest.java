package com.ficabridge.controller;

import com.ficabridge.config.SecurityConfig;
import com.ficabridge.controller.outbound.InvoiceController;
import com.ficabridge.exception.ResourceNotFoundException;
import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.service.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvoiceController.class)
@Import(SecurityConfig.class)
class InvoiceControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean InvoiceService invoiceService;

    // ── GET /api/invoices ────────────────────────────────────────────────────

    @Test
    void getInvoices_noParams_delegatesToGetAll() throws Exception {
        InvoiceDTO dto = invoiceDto("90001234", "100200", InvoiceStatus.OPEN);
        when(invoiceService.getAll()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].billingDocNumber", is("90001234")))
                .andExpect(jsonPath("$[0].status", is("OPEN")));

        verify(invoiceService).getAll();
        verifyNoMoreInteractions(invoiceService);
    }

    @Test
    void getInvoices_contractAccountOnly_delegatesToGetByContractAccount() throws Exception {
        InvoiceDTO dto = invoiceDto("90001234", "100200", InvoiceStatus.OPEN);
        when(invoiceService.getByContractAccount("100200")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/invoices").param("contractAccount", "100200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].contractAccount", is("100200")));

        verify(invoiceService).getByContractAccount("100200");
        verifyNoMoreInteractions(invoiceService);
    }

    @Test
    void getInvoices_statusOnly_delegatesToGetByStatus() throws Exception {
        InvoiceDTO dto = invoiceDto("90002345", "100201", InvoiceStatus.OVERDUE);
        when(invoiceService.getByStatus(InvoiceStatus.OVERDUE)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/invoices").param("status", "OVERDUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("OVERDUE")));

        verify(invoiceService).getByStatus(InvoiceStatus.OVERDUE);
        verifyNoMoreInteractions(invoiceService);
    }

    @Test
    void getInvoices_bothParams_delegatesToGetByContractAccountAndStatus() throws Exception {
        InvoiceDTO dto = invoiceDto("90003456", "100200", InvoiceStatus.OVERDUE);
        when(invoiceService.getByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE))
                .thenReturn(List.of(dto));

        mockMvc.perform(get("/api/invoices")
                        .param("contractAccount", "100200")
                        .param("status", "OVERDUE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(invoiceService).getByContractAccountAndStatus("100200", InvoiceStatus.OVERDUE);
        verifyNoMoreInteractions(invoiceService);
    }

    @Test
    void getInvoices_emptyResult_returns200WithEmptyArray() throws Exception {
        when(invoiceService.getAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getInvoices_multipleResults_returnsAll() throws Exception {
        when(invoiceService.getAll()).thenReturn(List.of(
                invoiceDto("90001111", "100200", InvoiceStatus.OPEN),
                invoiceDto("90002222", "100200", InvoiceStatus.CLEARED),
                invoiceDto("90003333", "100201", InvoiceStatus.OVERDUE)
        ));

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)));
    }

    // ── GET /api/invoices/{billingDocNumber} ─────────────────────────────────

    @Test
    void getInvoice_found_returns200WithDto() throws Exception {
        InvoiceDTO dto = invoiceDto("90001234", "100200", InvoiceStatus.CLEARED);
        dto.setAmount(new BigDecimal("1250.00"));
        dto.setCurrency("EUR");
        dto.setDueDate(LocalDate.of(2024, 4, 15));
        when(invoiceService.getByBillingDocNumber("90001234")).thenReturn(dto);

        mockMvc.perform(get("/api/invoices/90001234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billingDocNumber", is("90001234")))
                .andExpect(jsonPath("$.contractAccount", is("100200")))
                .andExpect(jsonPath("$.status", is("CLEARED")))
                .andExpect(jsonPath("$.amount", is(1250.00)))
                .andExpect(jsonPath("$.currency", is("EUR")));
    }

    @Test
    void getInvoice_notFound_returns404WithErrorBody() throws Exception {
        when(invoiceService.getByBillingDocNumber("UNKNOWN"))
                .thenThrow(new ResourceNotFoundException("Invoice not found: UNKNOWN"));

        mockMvc.perform(get("/api/invoices/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("UNKNOWN")));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private InvoiceDTO invoiceDto(String billingDocNumber, String contractAccount, InvoiceStatus status) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setBillingDocNumber(billingDocNumber);
        dto.setContractAccount(contractAccount);
        dto.setStatus(status);
        return dto;
    }
}
