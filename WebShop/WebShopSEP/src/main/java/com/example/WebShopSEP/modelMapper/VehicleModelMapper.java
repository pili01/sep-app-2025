package com.example.WebShopSEP.modelMapper;

import com.example.WebShopSEP.dto.vehicle.VehicleResponseDTO;
import com.example.WebShopSEP.model.Vehicle;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleModelMapper {
    VehicleResponseDTO toDto(Vehicle vehicle);

}
