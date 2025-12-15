package com.example.WebShopSEP.dto.vehicle;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VehicleCreateDTO {
    private Double pricePerDay;
    private String registration;
    private String chassisNumber;
    private String type;
    private String pictureUrl;
}


