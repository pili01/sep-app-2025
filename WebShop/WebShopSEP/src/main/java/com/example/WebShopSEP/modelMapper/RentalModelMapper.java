package com.example.WebShopSEP.modelMapper;

import com.example.WebShopSEP.dto.rental.RentalDto;
import com.example.WebShopSEP.model.Rental;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueMappingStrategy;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring", uses = {EquipmentModelMapper.class, InsuranceModelMapper.class, VehicleModelMapper.class, UserModelMapper.class}, nullValueMapMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
public interface RentalModelMapper {
    @Mapping(target = "vehicleId", source = "vehicle.id")
    @Mapping(target = "insuranceId", source = "insurance.id")
    RentalDto toDto(Rental rental);

    List<RentalDto> toDtoSet(List<Rental> rentals);
}
