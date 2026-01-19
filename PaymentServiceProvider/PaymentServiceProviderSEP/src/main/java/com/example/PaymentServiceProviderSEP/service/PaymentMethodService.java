package com.example.PaymentServiceProviderSEP.service;

import com.example.PaymentServiceProviderSEP.dto.paymentMethod.PaymentMethodConnectRequest;
import com.example.PaymentServiceProviderSEP.model.PaymentMethod;
import com.example.PaymentServiceProviderSEP.repository.PaymentMethodRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final SecureRestClientFactory restClientFactory;

    public boolean existsByName(String name){
        return paymentMethodRepository.existsByName(name);
    }

    @Transactional
    public void connect(PaymentMethodConnectRequest request) {

        PaymentMethod paymentMethod = paymentMethodRepository
                .findByName(request.getName())
                .orElseGet(() -> {
                    PaymentMethod pm = new PaymentMethod();
                    pm.setName(request.getName());
                    pm.setCheckIndex(0L);
                    pm.setActive(true);
                    return pm;
                });

        paymentMethod.setLastHeartbeat(LocalDateTime.now());

        if (!request.getHostname().equals(paymentMethod.getHostname())) {
            paymentMethod.setHostname(request.getHostname());
        }

        if (!request.getStatusUrl().equals(paymentMethod.getHealthEndpoint())) {
            paymentMethod.setHealthEndpoint(request.getStatusUrl());
        }

        if (!request.getPaymentUrl().equals(paymentMethod.getPaymentEndpoint())) {
            paymentMethod.setPaymentEndpoint(request.getPaymentUrl());
        }

        paymentMethodRepository.save(paymentMethod);
    }

    @Transactional
    public void heartbeatRoundRobin() {

        paymentMethodRepository.findFirstByOrderByCheckIndexAscIdAsc()
                .ifPresent(this::heartbeatAndIncrement);
    }

    private void heartbeatAndIncrement(PaymentMethod method) {

        var client = restClientFactory.create(method.getHostname());

        try {
            var response = client.get()
                    .uri(method.getHealthEndpoint())
                    .retrieve()
                    .toBodilessEntity();

            if (response != null) {
                System.out.println("Heartbeat response for " + method.getName() +
                        " (" + method.getHostname() + "): " +
                        response.getStatusCode());
            }

            method.setLastHeartbeat(LocalDateTime.now());

        } catch (Exception e) {
            System.err.println("Failed heartbeat for " + method.getName() +
                    " (" + method.getHostname() + "): " + e.getMessage());
        }

        method.setCheckIndex(method.getCheckIndex() + 1);
        paymentMethodRepository.save(method);
    }
}
