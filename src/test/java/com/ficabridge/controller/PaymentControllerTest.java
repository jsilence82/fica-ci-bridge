package com.ficabridge.controller;

import com.ficabridge.config.SecurityConfig;
import com.ficabridge.controller.outbound.PaymentController;
import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.service.OpenItemService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentController.class)
@Import(SecurityConfig.class)
class PaymentControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean OpenItemService openItemService;

    // ── GET /api/payments ────────────────────────────────────────────────────

    @Test
    void getOpenItems_noParams_delegatesToGetAllOpenItems() throws Exception {
        InvoiceDTO open = openItem("90001234", "100200", InvoiceStatus.OPEN, "850.00");
        InvoiceDTO overdue = openItem("90002345", "100201", InvoiceStatus.OVERDUE, "400.00");
        when(openItemService.getAllOpenItems(ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(open, overdue)));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(2)))
                .andExpect(jsonPath("$.content[0].invoiceNumber", is("90001234")))
                .andExpect(jsonPath("$.content[0].status", is("OPEN")))
                .andExpect(jsonPath("$.content[1].status", is("OVERDUE")))
                .andExpect(jsonPath("$.page.totalElements", is(2)));

        verify(openItemService).getAllOpenItems(ArgumentMatchers.any(Pageable.class));
        verifyNoMoreInteractions(openItemService);
    }

    @Test
    void getOpenItems_withContractAccount_delegatesToGetOpenItemsByContractAccount() throws Exception {
        InvoiceDTO dto = openItem("90001234", "100200", InvoiceStatus.OPEN, "850.00");
        when(openItemService.getOpenItemsByContractAccount(eq("100200"), ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/payments").param("contractAccount", "100200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].contractAccount", is("100200")));

        verify(openItemService).getOpenItemsByContractAccount(eq("100200"), ArgumentMatchers.any(Pageable.class));
        verifyNoMoreInteractions(openItemService);
    }

    @Test
    void getOpenItems_emptyResult_returns200WithEmptyContent() throws Exception {
        when(openItemService.getAllOpenItems(ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.page.totalElements", is(0)));
    }

    @Test
    void getOpenItems_contractAccountWithNoOpenItems_returns200WithEmptyContent() throws Exception {
        when(openItemService.getOpenItemsByContractAccount(eq("100200"), ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/payments").param("contractAccount", "100200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)));
    }

    @Test
    void getOpenItems_amountAndCurrencySerialised() throws Exception {
        InvoiceDTO dto = openItem("90001234", "100200", InvoiceStatus.OVERDUE, "1250.50");
        dto.setCurrency("EUR");
        when(openItemService.getAllOpenItems(ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto)));

        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount", is(1250.50)))
                .andExpect(jsonPath("$.content[0].currency", is("EUR")));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private InvoiceDTO openItem(String invoiceNumber, String contractAccount,
                                InvoiceStatus status, String amount) {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setInvoiceNumber(invoiceNumber);
        dto.setContractAccount(contractAccount);
        dto.setStatus(status);
        dto.setAmount(new BigDecimal(amount));
        return dto;
    }
}
