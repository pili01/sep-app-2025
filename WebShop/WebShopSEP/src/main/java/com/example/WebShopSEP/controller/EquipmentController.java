package com.example.WebShopSEP.controller;

import com.example.WebShopSEP.dto.equipment.EquipmentCreateDTO;
import com.example.WebShopSEP.dto.equipment.EquipmentResponseDTO;
import com.example.WebShopSEP.dto.equipment.EquipmentUpdateDTO;
import com.example.WebShopSEP.service.EquipmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Role;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/equipment")
public class EquipmentController {

    private final EquipmentService equipmentService;

    @Autowired
    public EquipmentController(EquipmentService equipmentService) {
        this.equipmentService = equipmentService;
    }

    @GetMapping
    public ResponseEntity<List<EquipmentResponseDTO>> getAllEquipment() {
        List<EquipmentResponseDTO> equipment = equipmentService.findAll();
        return ResponseEntity.ok(equipment);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EquipmentResponseDTO> getEquipmentById(@PathVariable Long id) {
        try {
            EquipmentResponseDTO equipment = equipmentService.findById(id);
            return ResponseEntity.ok(equipment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('AUTHOR')")
    @PostMapping
    public ResponseEntity<EquipmentResponseDTO> createEquipment(@RequestBody EquipmentCreateDTO equipmentCreateDTO) {
        EquipmentResponseDTO createdEquipment = equipmentService.save(equipmentCreateDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdEquipment);
    }

    @PreAuthorize("hasRole('AUTHOR')")
    @PutMapping("/{id}")
    public ResponseEntity<EquipmentResponseDTO> updateEquipment(@PathVariable Long id, @RequestBody EquipmentUpdateDTO equipmentUpdateDTO) {
        try {
            EquipmentResponseDTO updatedEquipment = equipmentService.update(id, equipmentUpdateDTO);
            return ResponseEntity.ok(updatedEquipment);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasRole('AUTHOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEquipment(@PathVariable Long id) {
        try {
            equipmentService.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }
}

