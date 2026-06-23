package com.cs2event.project.payment;

import com.cs2event.project.payment.dto.PixCharge;
import com.cs2event.project.team.Team;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;

@Component
public class AsaasClient {

    private static final String CREATE_STATIC_PIX_PATH = "/v3/pix/qrCodes/static";

    private final RestClient restClient;
    private final String pixKey;
    private final int pixExpiresSeconds;

    public AsaasClient(@Qualifier("asaasRestClient") RestClient restClient,
                       @Value("${asaas.pix-key}") String pixKey,
                       @Value("${asaas.pix-expires-seconds}") int pixExpiresSeconds) {
        this.restClient = restClient;
        this.pixKey = pixKey;
        this.pixExpiresSeconds = pixExpiresSeconds;
    }

    public PixCharge createPixCharge(Team team, int amountCents, String description) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("addressKey", pixKey);
        body.put("description", description);
        body.put("value", centsToReais(amountCents));
        body.put("format", "ALL");
        body.put("expirationSeconds", pixExpiresSeconds);
        body.put("allowsMultiplePayments", false);
        body.put("externalReference", team.getId().toString());

        try {
            CreateStaticPixResponse response = restClient.post()
                    .uri(CREATE_STATIC_PIX_PATH)
                    .body(body)
                    .retrieve()
                    .body(CreateStaticPixResponse.class);

            if (response == null || response.id() == null || response.payload() == null) {
                throw new AsaasException("Resposta vazia do Asaas ao criar QR Code Pix");
            }

            return new PixCharge(
                    response.id(),
                    response.payload(),
                    response.encodedImage(),
                    null,
                    amountCents
            );
        } catch (AsaasException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new AsaasException("Asaas recusou a criacao do QR Code Pix", e);
        } catch (Exception e) {
            throw new AsaasException("Falha ao criar QR Code Pix no Asaas", e);
        }
    }

    private BigDecimal centsToReais(int amountCents) {
        return BigDecimal.valueOf(amountCents)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.UNNECESSARY);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CreateStaticPixResponse(
            String id,
            String encodedImage,
            String payload,
            Boolean allowsMultiplePayments,
            String expirationDate,
            String externalReference,
            String description
    ) {
    }

    public static class AsaasException extends RuntimeException {
        public AsaasException(String message) {
            super(message);
        }

        public AsaasException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
