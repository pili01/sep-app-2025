package com.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayPalOrderDetailsResponse {

    private String id;
    private String status; // CREATED, APPROVED, VOIDED, COMPLETED, etc.

    @JsonProperty("purchase_units")
    private List<PurchaseUnit> purchaseUnits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PurchaseUnit {
        @JsonProperty("reference_id")
        private String referenceId;

        private Amount amount;

        @JsonProperty("payments")
        private Payments payments;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Amount {
        @JsonProperty("currency_code")
        private String currencyCode;
        private String value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Payments {
        private List<Capture> captures;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Capture {
        private String id;
        private String status; // COMPLETED, DECLINED, etc.
        private Amount amount;
    }
}
