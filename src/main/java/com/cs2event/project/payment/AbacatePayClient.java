package com.cs2event.project.payment;

import com.cs2event.project.payment.dto.PixCharge;
import com.cs2event.project.team.Team;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class AbacatePayClient {

    private static final Logger log = LoggerFactory.getLogger(AbacatePayClient.class);
    private static final String CREATE_PIX_PATH = "/v2/transparents/create";

    private final RestClient restClient;
    private final int pixExpiresSeconds;

    public AbacatePayClient(@Qualifier("abacatePayRestClient") RestClient restClient,
                            @Value("${abacatepay.pix-expires-seconds}") int pixExpiresSeconds) {
        this.restClient = restClient;
        this.pixExpiresSeconds = pixExpiresSeconds;
    }

    public PixCharge createPixCharge(Team team, int amountCents, String description) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("amount", amountCents);
        data.put("description", description);
        data.put("expiresIn", pixExpiresSeconds);
        data.put("externalId", team.getId().toString());
        Map<String, Object> body = Map.of("method", "PIX", "data", data);

        try {
            CreatePixResponse response = restClient.post()
                    .uri(CREATE_PIX_PATH)
                    .body(body)
                    .retrieve()
                    .body(CreatePixResponse.class);

            if (response == null || response.data() == null) {
                throw new AbacatePayException("Resposta vazia do AbacatePay ao criar cobrança Pix");
            }
            CreatePixResponse.Data data2 = response.data();
            return new PixCharge(
                    data2.id(),
                    data2.brCode(),
                    data2.brCodeBase64(),
                    data2.paymentUrl(),
                    amountCents
            );
        } catch (AbacatePayException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha ao criar cobrança Pix no AbacatePay para equipe {}", team.getTeamName(), e);
            throw new AbacatePayException("Falha ao criar cobrança Pix no AbacatePay", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CreatePixResponse(Data data, boolean success, String error) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Data(
                String id,
                String brCode,
                String brCodeBase64,
                @JsonAlias({"receiptUrl", "url", "checkoutUrl", "checkoutLink", "paymentUrl"}) String paymentUrl,
                String status
        ) {
        }
    }

    public static class AbacatePayException extends RuntimeException {
        public AbacatePayException(String message) {
            super(message);
        }

        public AbacatePayException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
