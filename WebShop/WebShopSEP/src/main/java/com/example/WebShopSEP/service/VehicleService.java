package com.example.WebShopSEP.service;

import com.example.WebShopSEP.dto.vehicle.VehicleCreateDTO;
import com.example.WebShopSEP.dto.vehicle.VehicleResponseDTO;
import com.example.WebShopSEP.dto.vehicle.VehicleUpdateDTO;
import com.example.WebShopSEP.model.Vehicle;
import com.example.WebShopSEP.repository.VehicleRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class VehicleService {

    private final VehicleRepository vehicleRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public VehicleService(VehicleRepository vehicleRepository, ModelMapper modelMapper) {
        this.vehicleRepository = vehicleRepository;
        this.modelMapper = modelMapper;
    }

    public List<VehicleResponseDTO> findAll() {
        return vehicleRepository.findAll().stream()
                .map(vehicle -> modelMapper.map(vehicle, VehicleResponseDTO.class))
                .collect(Collectors.toList());
    }

    public VehicleResponseDTO findById(Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));
        return modelMapper.map(vehicle, VehicleResponseDTO.class);
    }

    public VehicleResponseDTO save(VehicleCreateDTO vehicleCreateDTO) {
        Vehicle vehicle = modelMapper.map(vehicleCreateDTO, Vehicle.class);
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return modelMapper.map(savedVehicle, VehicleResponseDTO.class);
    }

    public VehicleResponseDTO update(Long id, VehicleUpdateDTO vehicleUpdateDTO) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Vehicle not found with id: " + id));

        modelMapper.map(vehicleUpdateDTO, vehicle);
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return modelMapper.map(updatedVehicle, VehicleResponseDTO.class);
    }

    public void deleteById(Long id) {
        if (!vehicleRepository.existsById(id)) {
            throw new RuntimeException("Vehicle not found with id: " + id);
        }
        vehicleRepository.deleteById(id);
    }
}

