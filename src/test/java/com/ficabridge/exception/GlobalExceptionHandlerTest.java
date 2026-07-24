package com.ficabridge.exception;

import com.ficabridge.config.SecurityConfig;
import com.ficabridge.controller.outbound.InvoiceController;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.service.InvoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.mockito.ArgumentMatchers;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InvoiceController.class)
@Import(SecurityConfig.class)
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean InvoiceService invoiceService;

    @Test
    void resourceNotFound_returns404WithStructuredBody() throws Exception {
        when(invoiceService.getByInvoiceNumber("MISSING"))
                .thenThrow(new ResourceNotFoundException("Invoice not found: MISSING"));

        mockMvc.perform(get("/api/invoices/MISSING"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.error", is("Not Found")))
                .andExpect(jsonPath("$.message", containsString("MISSING")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void oDataClientError_returns502WithStructuredBody() throws Exception {
        when(invoiceService.getByInvoiceNumber("DOC001"))
                .thenThrow(new ODataClientException("HTTP 503 from SAP", 503));

        mockMvc.perform(get("/api/invoices/DOC001"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status", is(502)))
                .andExpect(jsonPath("$.error", is("Bad Gateway")))
                .andExpect(jsonPath("$.message", containsString("SAP OData upstream error")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void invalidEnumQueryParam_returns400WithStructuredBody() throws Exception {
        mockMvc.perform(get("/api/invoices").param("status", "BOGUS"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.error", is("Bad Request")))
                .andExpect(jsonPath("$.message", containsString("BOGUS")))
                .andExpect(jsonPath("$.message", containsString("status")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void unexpectedException_returns500WithGenericMessage() throws Exception {
        when(invoiceService.getAll(ArgumentMatchers.any(Pageable.class)))
                .thenThrow(new RuntimeException("DB connection lost"));

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.error", is("Internal Server Error")))
                .andExpect(jsonPath("$.message", is("An unexpected error occurred")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void errorResponse_doesNotLeakInternalExceptionMessage() throws Exception {
        when(invoiceService.getAll(ArgumentMatchers.any(Pageable.class)))
                .thenThrow(new RuntimeException("sensitive internal detail"));

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message", not(containsString("sensitive internal detail"))));
    }

    @Test
    void validStatus_doesNotReturn400() throws Exception {
        when(invoiceService.getByStatus(eq(InvoiceStatus.OPEN), ArgumentMatchers.any(Pageable.class)))
                .thenReturn(Page.empty());

        mockMvc.perform(get("/api/invoices").param("status", "OPEN"))
                .andExpect(status().isOk());
    }
}
