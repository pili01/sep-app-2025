package com.example.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
public class PayPalOrderResponse {

    private String id;

    private String status;

    private List<Link> links;

    @Data
    public static class Link {
        private String href;
        private String rel;
        private String method;
    }

    public String getApprovalUrl() {
        if (links != null) {
            return links.stream()
                    .filter(link -> "approve".equals(link.getRel()))
                    .map(Link::getHref)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }
}
