package com.ficabridge.mapper;

import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceLineItemDTO;
import com.ficabridge.model.dto.InvoiceStatus;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.model.entity.InvoiceLineItemEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = {InvoiceMapperImpl.class})
class InvoiceMapperTest {

    @Autowired InvoiceMapper invoiceMapper;

    // ── toDto(InvoiceEntity) ──────────────────────────────────────────────────

    @Test
    void toDto_mapsAllScalarFields() {
        InvoiceEntity entity = invoiceEntity();

        InvoiceDTO dto = invoiceMapper.toDto(entity);

        assertThat(dto.getInvoiceNumber()).isEqualTo("90001001");
        assertThat(dto.getContractAccount()).isEqualTo("200001");
        assertThat(dto.getBusinessPartner()).isEqualTo("100001");
        assertThat(dto.getStatus()).isEqualTo(InvoiceStatus.OPEN);
        assertThat(dto.getAmount()).isEqualByComparingTo("143.40");
        assertThat(dto.getCurrency()).isEqualTo("EUR");
        assertThat(dto.getDueDate()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(dto.getOfficialDocumentNumber()).isEqualTo("ODN2025001001");
    }

    @Test
    void toDto_mapsLineItems() {
        InvoiceEntity entity = invoiceEntity();
        InvoiceLineItemEntity item = lineItemEntity(entity);
        entity.setLineItems(List.of(item));

        InvoiceDTO dto = invoiceMapper.toDto(entity);

        assertThat(dto.getLineItems()).hasSize(1);
        InvoiceLineItemDTO lineItem = dto.getLineItems().get(0);
        assertThat(lineItem.getItemNumber()).isEqualTo("10");
        assertThat(lineItem.getDescription()).isEqualTo("Electricity Q1");
        assertThat(lineItem.getMaterial()).isEqualTo("ELEC-BASE");
        assertThat(lineItem.getNetAmount()).isEqualByComparingTo("105.00");
        assertThat(lineItem.getCurrency()).isEqualTo("EUR");
        assertThat(lineItem.getChargingCategory()).isEqualTo("USAGE");
    }

    @Test
    void toDto_nullLineItems_resultHasNullLineItems() {
        InvoiceEntity entity = invoiceEntity();
        entity.setLineItems(null);

        InvoiceDTO dto = invoiceMapper.toDto(entity);

        assertThat(dto.getLineItems()).isNull();
    }

    @Test
    void toDto_emptyLineItems_resultHasEmptyList() {
        InvoiceEntity entity = invoiceEntity();
        entity.setLineItems(List.of());

        InvoiceDTO dto = invoiceMapper.toDto(entity);

        assertThat(dto.getLineItems()).isEmpty();
    }

    // ── toEntity(InvoiceDTO) ──────────────────────────────────────────────────

    @Test
    void toEntity_mapsScalarFields_ignoresIdCreatedAtLineItems() {
        InvoiceDTO dto = new InvoiceDTO();
        dto.setInvoiceNumber("90001001");
        dto.setContractAccount("200001");
        dto.setBusinessPartner("100001");
        dto.setStatus(InvoiceStatus.CLEARED);
        dto.setAmount(new BigDecimal("98.00"));
        dto.setCurrency("EUR");
        dto.setOfficialDocumentNumber("ODN2025001001");

        InvoiceEntity entity = invoiceMapper.toEntity(dto);

        assertThat(entity.getInvoiceNumber()).isEqualTo("90001001");
        assertThat(entity.getStatus()).isEqualTo(InvoiceStatus.CLEARED);
        assertThat(entity.getOfficialDocumentNumber()).isEqualTo("ODN2025001001");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getCreatedAt()).isNull();
        assertThat(entity.getLineItems()).isNull();
    }

    // ── toDtoList ─────────────────────────────────────────────────────────────

    @Test
    void toDtoList_emptyInput_returnsEmptyList() {
        assertThat(invoiceMapper.toDtoList(List.of())).isEmpty();
    }

    @Test
    void toDtoList_multipleEntities_returnsAll() {
        InvoiceEntity e1 = invoiceEntity();
        InvoiceEntity e2 = invoiceEntity();
        e2.setInvoiceNumber("90001002");

        List<InvoiceDTO> dtos = invoiceMapper.toDtoList(List.of(e1, e2));

        assertThat(dtos).hasSize(2);
        assertThat(dtos.get(1).getInvoiceNumber()).isEqualTo("90001002");
    }

    // ── toDto(InvoiceLineItemEntity) ──────────────────────────────────────────

    @Test
    void lineItemToDto_mapsAllFields() {
        InvoiceLineItemEntity item = lineItemEntity(null);

        InvoiceLineItemDTO dto = invoiceMapper.toDto(item);

        assertThat(dto.getItemNumber()).isEqualTo("10");
        assertThat(dto.getDescription()).isEqualTo("Electricity Q1");
        assertThat(dto.getMaterial()).isEqualTo("ELEC-BASE");
        assertThat(dto.getQuantity()).isEqualByComparingTo("350.000");
        assertThat(dto.getQuantityUnit()).isEqualTo("KWH");
        assertThat(dto.getNetAmount()).isEqualByComparingTo("105.00");
        assertThat(dto.getTaxAmount()).isEqualByComparingTo("19.95");
        assertThat(dto.getCurrency()).isEqualTo("EUR");
        assertThat(dto.getChargingCategory()).isEqualTo("USAGE");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private InvoiceEntity invoiceEntity() {
        InvoiceEntity e = new InvoiceEntity();
        e.setInvoiceNumber("90001001");
        e.setContractAccount("200001");
        e.setBusinessPartner("100001");
        e.setStatus(InvoiceStatus.OPEN);
        e.setAmount(new BigDecimal("143.40"));
        e.setCurrency("EUR");
        e.setDueDate(LocalDate.of(2025, 6, 1));
        e.setCreatedAt(LocalDateTime.now());
        e.setOfficialDocumentNumber("ODN2025001001");
        return e;
    }

    private InvoiceLineItemEntity lineItemEntity(InvoiceEntity parent) {
        InvoiceLineItemEntity item = new InvoiceLineItemEntity();
        item.setInvoice(parent);
        item.setItemNumber("10");
        item.setDescription("Electricity Q1");
        item.setMaterial("ELEC-BASE");
        item.setQuantity(new BigDecimal("350.000"));
        item.setQuantityUnit("KWH");
        item.setNetAmount(new BigDecimal("105.00"));
        item.setTaxAmount(new BigDecimal("19.95"));
        item.setCurrency("EUR");
        item.setChargingCategory("USAGE");
        return item;
    }
}
