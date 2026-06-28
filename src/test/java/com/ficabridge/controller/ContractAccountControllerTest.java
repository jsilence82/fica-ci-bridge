package com.ficabridge.controller;

import com.ficabridge.config.SecurityConfig;
import com.ficabridge.controller.outbound.ContractAccountController;
import com.ficabridge.exception.ResourceNotFoundException;
import com.ficabridge.model.dto.ContractAccountDTO;
import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.service.ContractAccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContractAccountController.class)
@Import(SecurityConfig.class)
class ContractAccountControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ContractAccountService contractAccountService;

    // ── GET /api/contract-accounts/{contractAccount} ─────────────────────────

    @Test
    void getContractAccount_found_returns200WithDto() throws Exception {
        ContractAccountDTO dto = contractAccountDto("100200", "5678");
        InvoiceDTO invoice = new InvoiceDTO();
        invoice.setBillingDocNumber("90001234");
        invoice.setStatus(InvoiceStatus.OPEN);
        dto.setInvoices(List.of(invoice));

        when(contractAccountService.getByContractAccount("100200")).thenReturn(dto);

        mockMvc.perform(get("/api/contract-accounts/100200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractAccount", is("100200")))
                .andExpect(jsonPath("$.businessPartner", is("5678")))
                .andExpect(jsonPath("$.invoices", hasSize(1)))
                .andExpect(jsonPath("$.invoices[0].billingDocNumber", is("90001234")));
    }

    @Test
    void getContractAccount_notFound_returns404WithErrorBody() throws Exception {
        when(contractAccountService.getByContractAccount("UNKNOWN"))
                .thenThrow(new ResourceNotFoundException("ContractAccount not found: UNKNOWN"));

        mockMvc.perform(get("/api/contract-accounts/UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("UNKNOWN")));
    }

    @Test
    void getContractAccount_noInvoices_returnsEmptyInvoiceList() throws Exception {
        ContractAccountDTO dto = contractAccountDto("100200", "5678");
        dto.setInvoices(List.of());

        when(contractAccountService.getByContractAccount("100200")).thenReturn(dto);

        mockMvc.perform(get("/api/contract-accounts/100200"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.invoices", hasSize(0)));
    }

    // ── GET /api/contract-accounts/overdue ───────────────────────────────────

    @Test
    void getOverdue_returnsAccountsWithOverdueInvoices() throws Exception {
        ContractAccountDTO ca1 = contractAccountDto("100200", "5678");
        InvoiceDTO overdueInvoice = new InvoiceDTO();
        overdueInvoice.setBillingDocNumber("90001234");
        overdueInvoice.setStatus(InvoiceStatus.OVERDUE);
        ca1.setInvoices(List.of(overdueInvoice));

        ContractAccountDTO ca2 = contractAccountDto("100201", "5679");
        ca2.setInvoices(List.of(new InvoiceDTO()));

        when(contractAccountService.getAllWithOverdueItems()).thenReturn(List.of(ca1, ca2));

        mockMvc.perform(get("/api/contract-accounts/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].contractAccount", is("100200")))
                .andExpect(jsonPath("$[0].invoices[0].status", is("OVERDUE")));
    }

    @Test
    void getOverdue_noOverdueAccounts_returns200WithEmptyArray() throws Exception {
        when(contractAccountService.getAllWithOverdueItems()).thenReturn(List.of());

        mockMvc.perform(get("/api/contract-accounts/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private ContractAccountDTO contractAccountDto(String contractAccount, String businessPartner) {
        ContractAccountDTO dto = new ContractAccountDTO();
        dto.setContractAccount(contractAccount);
        dto.setBusinessPartner(businessPartner);
        return dto;
    }
}
