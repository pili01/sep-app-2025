package com.example.PaymentServiceProviderSEP.dto.paymentMethod;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentMethodDTO {

    private Long id;
    private String name;
    private String paymentMethodCode;
    private Boolean active;
    private Boolean enabled;
    private LocalDateTime lastHeartbeat;
    private String hostname;
    private String healthEndpoint;
    private String paymentEndpoint;
    private LocalDateTime createdAt;
    private String iconPath;

    // Constructor for backward compatibility
    public PaymentMethodDTO(Long id, String name, String paymentMethodCode, Boolean active, Boolean enabled, LocalDateTime lastHeartbeat) {
        this.id = id;
        this.name = name;
        this.paymentMethodCode = paymentMethodCode;
        this.active = active;
        this.enabled = enabled;
        this.lastHeartbeat = lastHeartbeat;
    }
}