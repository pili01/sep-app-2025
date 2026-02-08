package com.example.PaymentServiceProviderSEP;

import com.example.PaymentServiceProviderSEP.service.PaymentMethodService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentMethodInitializer implements ApplicationRunner {

    private final PaymentMethodService paymentMethodService;

    @Override
    public void run(ApplicationArguments args) {
        paymentMethodService.ensureInternalPaymentMethodsExist();
    }
}