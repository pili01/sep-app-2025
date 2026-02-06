package com.example.PaymentServiceProviderSEP.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Dozvoljava zahtjeve samo ako dolaze preko nginx-a (nginx šalje tajni header).
 * Ako je app.internal-gateway.secret postavljen, zahtjev mora imati X-Internal-Gateway sa tim vrijednošću.
 * Ako secret nije postavljen (prazan), filter se ne primjenjuje - korisno za lokalni dev bez nginx-a.
 */
@Component
public class NginxGatewayFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Gateway";

    @Value("${app.internal-gateway.secret:}")
    private String expectedSecret;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (expectedSecret == null || expectedSecret.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        String received = request.getHeader(HEADER_NAME);
        if (!expectedSecret.equals(received)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"Forbidden: direct access not allowed\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}
