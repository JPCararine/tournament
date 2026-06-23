package com.cs2event.project.payment;

import com.cs2event.project.payment.dto.PixCharge;
import com.cs2event.project.team.Team;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Adaptador HTTP para o AbacatePay. Concentra TODO o conhecimento do formato da
 * API do gateway — o restante da aplicação fala apenas em {@link PixCharge}.
 *
 * <p>[confirmar na doc AbacatePay] o endpoint exato de criação de cobrança Pix,
 * os nomes dos campos de request/response e o evento de webhook. Os valores
 * abaixo são os mais prováveis segundo a documentação pública; ajuste aqui
 * caso divirjam — nenhum outro arquivo precisa mudar.</p>
 */
@Component
public class AbacatePayClient {

    private static final Logger log = LoggerFactory.getLogger(AbacatePayClient.class);

    /** [confirmar na doc] endpoint de criação de cobrança Pix dinâmica. */
    private static final String CREATE_PIX_PATH = "/v1/pixQrCode/create";

    private final RestClient restClient;

    public AbacatePayClient(@Qualifier("abacatePayRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Cria uma cobrança Pix dinâmica para a equipe.
     *
     * @throws AbacatePayException se a chamada falhar
     */
    public PixCharge createPixCharge(Team team, int amountCents, String description) {
        // [confirmar na doc] nomes dos campos do corpo da requisição.
        Map<String, Object> body = Map.of(
                "amount", amountCents,
                "description", description,
                "expiresIn", 3600,
                "customer", Map.of(
                        "name", team.getCaptainName(),
                        "email", team.getCaptainEmail(),
                        "cellphone", team.getWhatsapp()
                )
        );

        try {
            CreatePixResponse response = restClient.post()
                    .uri(CREATE_PIX_PATH)
                    .body(body)
                    .retrieve()
                    .body(CreatePixResponse.class);

            if (response == null || response.data() == null) {
                throw new AbacatePayException("Resposta vazia do AbacatePay ao criar cobrança Pix");
            }
            CreatePixResponse.Data data = response.data();
            return new PixCharge(
                    data.id(),
                    data.brCode(),
                    data.brCodeBase64(),
                    data.url(),
                    amountCents
            );
        } catch (AbacatePayException e) {
            throw e;
        } catch (Exception e) {
            log.error("Falha ao criar cobrança Pix no AbacatePay para equipe {}", team.getTeamName(), e);
            throw new AbacatePayException("Falha ao criar cobrança Pix no AbacatePay", e);
        }
    }

    /** [confirmar na doc] formato da resposta de criação de cobrança Pix. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CreatePixResponse(Data data) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        private record Data(String id, String brCode, String brCodeBase64, String url) {
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
