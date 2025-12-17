package com.example.WebShopSEP.controller;

import com.example.WebShopSEP.dto.vehicle.VehicleCreateDTO;
import com.example.WebShopSEP.dto.vehicle.VehicleResponseDTO;
import com.example.WebShopSEP.dto.vehicle.VehicleUpdateDTO;
import com.example.WebShopSEP.service.VehicleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleService vehicleService;

    @Autowired
    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    @GetMapping
    public ResponseEntity<List<VehicleResponseDTO>> getAllVehicles() {
        List<VehicleResponseDTO> vehicles = vehicleService.findAll();
        return ResponseEntity.ok(vehicles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> getVehicleById(@PathVariable Integer id) {
        try {
            VehicleResponseDTO vehicle = vehicleService.findById(id);
            return ResponseEntity.ok(vehicle);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('AUTHOR')")
    @PostMapping
    public ResponseEntity<VehicleResponseDTO> createVehicle(@RequestBody VehicleCreateDTO vehicleCreateDTO) {
        VehicleResponseDTO createdVehicle = vehicleService.save(vehicleCreateDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdVehicle);
    }

    @PreAuthorize("hasRole('AUTHOR')")
    @PutMapping("/{id}")
    public ResponseEntity<VehicleResponseDTO> updateVehicle(@PathVariable Integer id, @RequestBody VehicleUpdateDTO vehicleUpdateDTO) {
        try {
            VehicleResponseDTO updatedVehicle = vehicleService.update(id, vehicleUpdateDTO);
            return ResponseEntity.ok(updatedVehicle);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('AUTHOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVehicle(@PathVariable Integer id) {
        try {
            vehicleService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

