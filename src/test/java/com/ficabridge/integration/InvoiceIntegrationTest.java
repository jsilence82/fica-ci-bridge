package com.ficabridge.integration;

import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.repository.InvoiceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class InvoiceIntegrationTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private InvoiceRepository invoiceRepository;

    @BeforeEach
    void setUp() {
        invoiceRepository.deleteAll();
        invoiceRepository.save(invoice("90001001", "200001", "100001",
                InvoiceStatus.OPEN, "143.40", "EUR", LocalDate.now().plusDays(30)));
        invoiceRepository.save(invoice("90001002", "200001", "100001",
                InvoiceStatus.OVERDUE, "98.00", "EUR", LocalDate.now().minusDays(10)));
        invoiceRepository.save(invoice("90002001", "200002", "100002",
                InvoiceStatus.CLEARED, "220.00", "EUR", LocalDate.now().minusDays(60)));
    }

    // ── GET /api/invoices (paged) ─────────────────────────────────────────────

    @Test
    void getInvoices_noFilter_returnsAllSeededInvoices() {
        assertThat(content(getPaged("/api/invoices"))).hasSize(3);
    }

    @Test
    void getInvoices_byContractAccount_returnsOnlyMatchingInvoices() {
        assertThat(content(getPaged("/api/invoices?contractAccount=200001"))).hasSize(2);
    }

    @Test
    void getInvoices_byStatus_returnsOnlyMatchingStatus() {
        assertThat(content(getPaged("/api/invoices?status=OVERDUE"))).hasSize(1);
    }

    @Test
    void getInvoices_byContractAccountAndStatus_returnsIntersection() {
        assertThat(content(getPaged("/api/invoices?contractAccount=200001&status=OPEN"))).hasSize(1);
    }

    @Test
    void getInvoices_noMatchingContractAccount_returnsEmptyContent() {
        assertThat(content(getPaged("/api/invoices?contractAccount=UNKNOWN"))).isEmpty();
    }

    @Test
    void getInvoices_responseIncludesPagingMetadata() {
        Map<String, Object> page = pageMeta(getPaged("/api/invoices"));

        assertThat(page.get("number")).isEqualTo(0);
        assertThat(page.get("size")).isEqualTo(50);          // default-page-size when size unspecified
        assertThat(page.get("totalElements")).isEqualTo(3);
        assertThat(page.get("totalPages")).isEqualTo(1);
    }

    @Test
    void getInvoices_secondPage_honoursPageAndSizeParams() {
        Map<String, Object> body = getPaged("/api/invoices?page=1&size=2");

        assertThat(content(body)).hasSize(1);                // 3 total, 2 per page → 1 on page index 1
        assertThat(pageMeta(body).get("number")).isEqualTo(1);
        assertThat(pageMeta(body).get("size")).isEqualTo(2);
        assertThat(pageMeta(body).get("totalPages")).isEqualTo(2);
    }

    @Test
    void getInvoices_sizeAboveMax_isClampedTo200() {
        // max-page-size is 200; an absurd size must not force an effectively unbounded query
        assertThat(pageMeta(getPaged("/api/invoices?size=9999")).get("size")).isEqualTo(200);
    }

    // ── GET /api/invoices/{invoiceNumber} ──────────────────────────────────

    @SuppressWarnings("unchecked")
    @Test
    void getInvoice_found_returns200WithCorrectFields() {
        ResponseEntity<Map<String, Object>> response =
                restTemplate.getForEntity("/api/invoices/90001001", (Class<Map<String, Object>>) (Class<?>) Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .containsEntry("invoiceNumber", "90001001")
                .containsEntry("contractAccount", "200001")
                .containsEntry("status", "OPEN")
                .containsEntry("currency", "EUR");
    }

    @SuppressWarnings("unchecked")
    @Test
    void getInvoice_notFound_returns404WithErrorField() {
        ResponseEntity<Map<String, Object>> response =
                restTemplate.getForEntity("/api/invoices/BADID", (Class<Map<String, Object>>) (Class<?>) Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("error");
    }

    // ── GET /api/payments (paged) ─────────────────────────────────────────────

    @Test
    void getPayments_noFilter_returnsOpenAndOverdueOnly() {
        // CLEARED invoice must not be included
        assertThat(content(getPaged("/api/payments"))).hasSize(2);
    }

    @Test
    void getPayments_byContractAccount_returnsFilteredOpenItems() {
        assertThat(content(getPaged("/api/payments?contractAccount=200001"))).hasSize(2);
    }

    @Test
    void getPayments_contractAccountWithNoOpenItems_returnsEmptyContent() {
        // CA 200002 only has a CLEARED invoice — no open items
        assertThat(content(getPaged("/api/payments?contractAccount=200002"))).isEmpty();
    }

    @Test
    void getPayments_responseIncludesPagingMetadata() {
        Map<String, Object> page = pageMeta(getPaged("/api/payments"));

        assertThat(page.get("number")).isEqualTo(0);
        assertThat(page.get("size")).isEqualTo(50);
        assertThat(page.get("totalElements")).isEqualTo(2);
    }

    @Test
    void getPayments_sizeAboveMax_isClampedTo200() {
        assertThat(pageMeta(getPaged("/api/payments?size=9999")).get("size")).isEqualTo(200);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> getPaged(String url) {
        ResponseEntity<Map<String, Object>> response =
                restTemplate.getForEntity(url, (Class<Map<String, Object>>) (Class<?>) Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return response.getBody();
    }

    @SuppressWarnings("unchecked")
    private static List<Object> content(Map<String, Object> body) {
        return (List<Object>) body.get("content");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> pageMeta(Map<String, Object> body) {
        return (Map<String, Object>) body.get("page");
    }

    private InvoiceEntity invoice(String invoiceNumber,
                                  String contractAccount, String businessPartner,
                                  InvoiceStatus status, String amount, String currency,
                                  LocalDate dueDate) {
        InvoiceEntity e = new InvoiceEntity();
        e.setInvoiceNumber(invoiceNumber);
        e.setContractAccount(contractAccount);
        e.setBusinessPartner(businessPartner);
        e.setStatus(status);
        e.setAmount(new BigDecimal(amount));
        e.setCurrency(currency);
        e.setDueDate(dueDate);
        return e;
    }
}
