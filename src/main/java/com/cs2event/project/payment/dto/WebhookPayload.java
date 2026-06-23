package com.cs2event.project.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Locale;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WebhookPayload(
        String event,
        String status,
        @JsonProperty("billingId") String billingId,
        Data data
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Data(
            String id,
            @JsonProperty("billingId") String billingId,
            String status,
            PixQrCode pixQrCode,
            Transparent transparent
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record PixQrCode(String id, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transparent(String id, String externalId, String status) {
    }

    /**
     * Identificador da cobrança (igual ao {@code billingId} salvo na equipe).
     * No checkout transparente vem em {@code data.transparent.id}.
     */
    public String resolveBillingId() {
        if (hasText(billingId)) {
            return billingId;
        }
        if (data != null) {
            if (data.transparent() != null && hasText(data.transparent().id())) {
                return data.transparent().id();
            }
            if (hasText(data.billingId())) {
                return data.billingId();
            }
            if (data.pixQrCode() != null && hasText(data.pixQrCode().id())) {
                return data.pixQrCode().id();
            }
            if (hasText(data.id())) {
                return data.id();
            }
        }
        return null;
    }

    /**
     * {@code externalId} enviado na criação da cobrança (o UUID da equipe).
     * Usado como fallback caso o billingId não case.
     */
    public String resolveExternalId() {
        if (data != null && data.transparent() != null) {
            return data.transparent().externalId();
        }
        return null;
    }

    /** Indica se o evento representa um pagamento efetivado. */
    public boolean isPaid() {
        String e = event == null ? "" : event.toLowerCase(Locale.ROOT);
        if (e.endsWith(".paid") || e.endsWith(".completed")) {
            return true;
        }
        return "PAID".equalsIgnoreCase(effectiveStatus());
    }

    private String effectiveStatus() {
        if (data != null) {
            if (data.transparent() != null && hasText(data.transparent().status())) {
                return data.transparent().status();
            }
            if (hasText(data.status())) {
                return data.status();
            }
        }
        return status;
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
