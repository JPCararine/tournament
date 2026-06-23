package com.cs2event.project.payment.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Payload do webhook do AbacatePay (Fluxo B).
 *
 * <p>[confirmar na doc AbacatePay] o formato exato. O modelo abaixo é tolerante
 * a campos extras e tenta extrair o id da cobrança tanto do topo quanto de um
 * objeto aninhado {@code data}.</p>
 */
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
            String status
    ) {
    }

    /** Resolve o id da cobrança considerando as variações de payload. */
    public String resolveBillingId() {
        if (billingId != null && !billingId.isBlank()) {
            return billingId;
        }
        if (data != null) {
            if (data.billingId() != null && !data.billingId().isBlank()) {
                return data.billingId();
            }
            if (data.id() != null && !data.id().isBlank()) {
                return data.id();
            }
        }
        return null;
    }
}
