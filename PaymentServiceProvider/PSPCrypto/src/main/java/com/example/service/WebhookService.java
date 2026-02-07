package com.example.service;

import com.example.model.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookService {
    private final PSPCoreClient pspCoreClient;

    @Async
    public void notifyPaymentCompleted(Transaction transaction) {
        try {
            pspCoreClient.notifyPSP(transaction);
        } catch (Exception e) {
            log.error("Failed to send webhook notification", e);
        }
    }

    @Async
    public void notifyPaymentFailed(Transaction transaction) {
        try {
            pspCoreClient.notifyPaymentFailed(transaction);
        } catch (Exception e) {
            log.error("Failed to send webhook notification", e);
        }
    }

    @Async
    public void notifyPaymentError(Transaction transaction) {
        try {
            pspCoreClient.notifyPaymentError(transaction);
        } catch (Exception e) {
            log.error("Failed to send webhook notification", e);
        }
    }
}
