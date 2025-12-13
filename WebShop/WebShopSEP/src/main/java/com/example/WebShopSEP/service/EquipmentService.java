package com.example.WebShopSEP.service;

import com.example.WebShopSEP.dto.equipment.EquipmentCreateDTO;
import com.example.WebShopSEP.dto.equipment.EquipmentResponseDTO;
import com.example.WebShopSEP.dto.equipment.EquipmentUpdateDTO;
import com.example.WebShopSEP.model.Equipment;
import com.example.WebShopSEP.repository.EquipmentRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class EquipmentService {

    private final EquipmentRepository equipmentRepository;
    private final ModelMapper modelMapper;

    @Autowired
    public EquipmentService(EquipmentRepository equipmentRepository, ModelMapper modelMapper) {
        this.equipmentRepository = equipmentRepository;
        this.modelMapper = modelMapper;
    }

    public List<EquipmentResponseDTO> findAll() {
        return equipmentRepository.findAll().stream()
                .map(equipment -> modelMapper.map(equipment, EquipmentResponseDTO.class))
                .collect(Collectors.toList());
    }

    public EquipmentResponseDTO findById(Integer id) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));
        return modelMapper.map(equipment, EquipmentResponseDTO.class);
    }

    public EquipmentResponseDTO save(EquipmentCreateDTO equipmentCreateDTO) {
        Equipment equipment = modelMapper.map(equipmentCreateDTO, Equipment.class);
        Equipment savedEquipment = equipmentRepository.save(equipment);
        return modelMapper.map(savedEquipment, EquipmentResponseDTO.class);
    }

    public EquipmentResponseDTO update(Integer id, EquipmentUpdateDTO equipmentUpdateDTO) {
        Equipment equipment = equipmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Equipment not found with id: " + id));

        modelMapper.map(equipmentUpdateDTO, equipment);
        Equipment updatedEquipment = equipmentRepository.save(equipment);
        return modelMapper.map(updatedEquipment, EquipmentResponseDTO.class);
    }

    public void deleteById(Integer id) {
        if (!equipmentRepository.existsById(id)) {
            throw new RuntimeException("Equipment not found with id: " + id);
        }
        equipmentRepository.deleteById(id);
    }
}

