package com.example.Bank.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class AuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("AUDIT");

    private final AuditHashService hashService;

    public AuditLogger(AuditHashService hashService) {
        this.hashService = hashService;
    }

    public void info(String message) {
        String hash = hashService.calculateHash(message);
        MDC.put("auditHash", hash);
        auditLog.info(message);
        MDC.remove("auditHash");
    }
}