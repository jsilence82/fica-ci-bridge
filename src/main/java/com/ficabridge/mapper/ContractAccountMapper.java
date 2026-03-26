package com.ficabridge.mapper;

import com.ficabridge.model.dto.ContractAccountDTO;
import com.ficabridge.model.entity.ContractAccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ContractAccountMapper {

    @Mapping(target = "invoices", ignore = true)
    ContractAccountDTO toDto(ContractAccountEntity entity);

    @Mapping(target = "id", ignore = true)
    ContractAccountEntity toEntity(ContractAccountDTO dto);
}
