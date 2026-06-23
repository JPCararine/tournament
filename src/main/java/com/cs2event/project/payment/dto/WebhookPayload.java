package com.cs2event.project.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookPayload(
        String id,
        String event,
        Payment payment
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Payment(
            String id,
            String status,
            String externalReference,
            String pixQrCodeId
    ) {
    }

    /**
     * Para QR Code estatico, o Asaas envia o id do QR pago em payment.pixQrCodeId.
     * Esse valor e o billingId salvo na equipe durante a criacao do QR Code.
     */
    public String resolveBillingId() {
        if (payment == null) {
            return null;
        }
        if (hasText(payment.pixQrCodeId())) {
            return payment.pixQrCodeId();
        }
        return payment.id();
    }

    public String resolveExternalId() {
        return payment == null ? null : payment.externalReference();
    }

    public boolean isPaid() {
        String normalizedEvent = event == null ? "" : event.toUpperCase(Locale.ROOT);
        if ("PAYMENT_RECEIVED".equals(normalizedEvent) || "PAYMENT_CONFIRMED".equals(normalizedEvent)) {
            return true;
        }

        String status = payment == null ? "" : payment.status();
        return "RECEIVED".equalsIgnoreCase(status) || "CONFIRMED".equalsIgnoreCase(status);
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
