package com.example.WebShopSEP.dto.equipment;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EquipmentResponseDTO {
    private Integer id;
    private String name;
    private String description;
    private Double pricePerDay;
}


