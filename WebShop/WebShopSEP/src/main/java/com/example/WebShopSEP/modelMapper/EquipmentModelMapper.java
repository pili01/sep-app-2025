package com.example.WebShopSEP.modelMapper;

import com.example.WebShopSEP.dto.equipment.EquipmentResponseDTO;
import com.example.WebShopSEP.model.Equipment;
import org.mapstruct.Mapper;

import java.util.Set;

@Mapper(componentModel = "spring")
public interface EquipmentModelMapper {
    EquipmentResponseDTO toDto(Equipment equipment);
    Set<EquipmentResponseDTO> toDtoSet(Set<Equipment> equipmentSet);

    Equipment toEntity(EquipmentResponseDTO equipmentResponseDTO);
    Set<Equipment> toEntitySet(Set<EquipmentResponseDTO> equipmentResponseDTOSet);
}
