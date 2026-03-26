package com.ficabridge.mapper;

import com.ficabridge.model.dto.InvoiceDTO;
import com.ficabridge.model.entity.InvoiceEntity;
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

    List<InvoiceDTO> toDtoList(List<InvoiceEntity> entities);
}
