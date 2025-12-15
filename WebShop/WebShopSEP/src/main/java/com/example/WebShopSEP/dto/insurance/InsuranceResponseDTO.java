package com.example.WebShopSEP.dto.insurance;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InsuranceResponseDTO {
    private Integer id;
    private String company;
    private String name;
    private Double pricePerDay;
}


