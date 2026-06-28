package com.ficabridge.mapper;

import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.dto.InvoiceLineItemDTO;
import com.ficabridge.model.entity.InvoiceEntity;
import com.ficabridge.model.entity.InvoiceLineItemEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "lineItems", ignore = true)
    InvoiceEntity toEntity(InvoiceDTO dto);

    InvoiceDTO toDto(InvoiceEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    InvoiceLineItemEntity toEntity(InvoiceLineItemDTO dto);

    InvoiceLineItemDTO toDto(InvoiceLineItemEntity entity);

    List<InvoiceDTO> toDtoList(List<InvoiceEntity> entities);
}
