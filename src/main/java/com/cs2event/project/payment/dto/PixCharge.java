package com.cs2event.project.payment.dto;

public record PixCharge(
        String billingId,
        String brCode,
        String brCodeBase64,
        String url,
        int amountCents
) {
}
