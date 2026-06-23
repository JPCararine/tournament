package com.cs2event.project.payment.dto;

/**
 * Resultado da criação de uma cobrança Pix, normalizado para o restante da
 * aplicação (independente do formato do AbacatePay).
 *
 * @param billingId    id da cobrança no AbacatePay (chave para casar o webhook)
 * @param brCode       código Pix copia-e-cola
 * @param brCodeBase64 imagem do QR-code em base64 (data URI ou base64 puro)
 * @param url          link da cobrança (checkout AbacatePay)
 * @param amountCents  valor cobrado em centavos
 */
public record PixCharge(
        String billingId,
        String brCode,
        String brCodeBase64,
        String url,
        int amountCents
) {
}
