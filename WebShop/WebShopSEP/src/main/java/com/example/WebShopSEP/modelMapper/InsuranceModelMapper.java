package com.example.WebShopSEP.modelMapper;

import com.example.WebShopSEP.dto.insurance.InsuranceResponseDTO;
import com.example.WebShopSEP.model.Insurance;
import org.mapstruct.Mapper;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface InsuranceModelMapper {
    InsuranceResponseDTO toDto(Insurance insurance);
    Set<InsuranceResponseDTO> toDtoSet(Set<Insurance> insuranceSet);
}
