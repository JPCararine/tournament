package com.cs2event.project.payment;

import com.cs2event.project.payment.dto.WebhookPayload;
import com.cs2event.project.team.Team;
import com.cs2event.project.team.TeamRepository;
import com.cs2event.project.team.TeamStatus;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Recebe a confirmação de pagamento do AbacatePay (Fluxo B).
 *
 * <p>Valida o secret, é idempotente e só ele pode promover PENDENTE → CONFIRMADA.</p>
 */
@RestController
@RequestMapping("/api/webhooks/abacatepay")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final TeamRepository teamRepository;
    private final String webhookSecret;

    public PaymentWebhookController(TeamRepository teamRepository,
                                    @Value("${abacatepay.webhook-secret}") String webhookSecret) {
        this.teamRepository = teamRepository;
        this.webhookSecret = webhookSecret;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Void> handle(
            @RequestParam(name = "webhookSecret", required = false) String secretParam,
            @RequestHeader(name = "X-Webhook-Secret", required = false) String secretHeader,
            @RequestBody WebhookPayload payload) {

        // 1) Autenticidade: aceita o secret via query param OU header.
        if (!isAuthentic(secretParam, secretHeader)) {
            log.warn("Webhook AbacatePay rejeitado: secret inválido/ausente");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // 2) Localiza a cobrança.
        String billingId = payload == null ? null : payload.resolveBillingId();
        if (!StringUtils.hasText(billingId)) {
            log.warn("Webhook AbacatePay sem billingId reconhecível: {}", payload);
            return ResponseEntity.badRequest().build();
        }

        Optional<Team> maybeTeam = teamRepository.findByBillingId(billingId);
        if (maybeTeam.isEmpty()) {
            // Cobrança desconhecida — responde 200 para o gateway não ficar reenviando.
            log.warn("Webhook AbacatePay: nenhuma equipe para billingId={}", billingId);
            return ResponseEntity.ok().build();
        }

        Team team = maybeTeam.get();

        // 3) Idempotência: já confirmada → no-op.
        if (team.getStatus() == TeamStatus.CONFIRMADA) {
            log.info("Webhook AbacatePay duplicado para equipe '{}' (billingId={}) — ignorado",
                    team.getTeamName(), billingId);
            return ResponseEntity.ok().build();
        }

        // 4) Promove PENDENTE → CONFIRMADA.
        team.setStatus(TeamStatus.CONFIRMADA);
        team.setConfirmedAt(Instant.now());
        teamRepository.save(team);
        log.info("Equipe '{}' CONFIRMADA via webhook (billingId={})", team.getTeamName(), billingId);
        return ResponseEntity.ok().build();
    }

    private boolean isAuthentic(String secretParam, String secretHeader) {
        if (!StringUtils.hasText(webhookSecret)) {
            // Configuração ausente: trata como bloqueio, nunca como liberação.
            log.error("abacatepay.webhook-secret não configurado — rejeitando webhook");
            return false;
        }
        return webhookSecret.equals(secretParam) || webhookSecret.equals(secretHeader);
    }
}
