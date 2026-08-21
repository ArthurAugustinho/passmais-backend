package com.passmais.infrastructure.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendVerificationEmail(String to, String code) {
        // Stub: log instead of actual send. Integrate SMTP in production.
        log.info("[EMAIL] To: {} | Subject: Verify your account | Code: {}", to, code);
    }
}

