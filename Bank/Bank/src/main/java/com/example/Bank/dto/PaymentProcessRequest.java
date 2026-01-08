package com.example.Bank.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentProcessRequest {
    @NotBlank(message = "PAN is required")
    private String pan;
    
    @NotBlank(message = "Security code is required")
    @Pattern(regexp = "\\d{3,4}", message = "Security code must be 3 or 4 digits")
    private String securityCode; // cvv
    
    @NotBlank(message = "Card holder name is required")
    private String cardHolderName;
    
    @NotBlank(message = "Expiration date is required")
    @Pattern(regexp = "\\d{2}/\\d{2}", message = "Expiration date must be in MM/YY format")
    private String expirationDate; // mm/yy format
}





