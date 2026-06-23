package com.cs2event.project.payment;

import com.cs2event.project.payment.dto.WebhookPayload;
import com.cs2event.project.team.Team;
import com.cs2event.project.team.TeamRepository;
import com.cs2event.project.team.TeamStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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

@RestController
@RequestMapping("/webhooks/asaas")
public class PaymentWebhookController {

    private static final Logger log = LoggerFactory.getLogger(PaymentWebhookController.class);

    private final TeamRepository teamRepository;
    private final String webhookToken;

    public PaymentWebhookController(TeamRepository teamRepository,
                                    @Value("${asaas.webhook-token}") String webhookToken) {
        this.teamRepository = teamRepository;
        this.webhookToken = webhookToken;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<Void> handle(
            @RequestParam(name = "webhookSecret", required = false) String secretParam,
            @RequestHeader(name = "X-Webhook-Secret", required = false) String secretHeader,
            @RequestHeader(name = "asaas-access-token", required = false) String asaasAccessToken,
            @RequestBody WebhookPayload payload) {
        if (!isAuthentic(asaasAccessToken, secretHeader, secretParam)) {
            log.warn("Webhook Asaas rejeitado: token invalido/ausente");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (payload == null) {
            log.warn("Webhook Asaas com corpo vazio");
            return ResponseEntity.badRequest().build();
        }

        String billingId = payload.resolveBillingId();
        String externalId = payload.resolveExternalId();
        Optional<Team> maybeTeam = findTeam(billingId, externalId);
        if (maybeTeam.isEmpty()) {
            log.warn("Webhook Asaas: nenhuma equipe para billingId={} externalReference={}", billingId, externalId);
            return ResponseEntity.ok().build();
        }

        if (!payload.isPaid()) {
            log.info("Webhook Asaas ignorado (evento nao pago): event={} billingId={}",
                    payload.event(), billingId);
            return ResponseEntity.ok().build();
        }

        Team team = maybeTeam.get();
        if (team.getStatus() == TeamStatus.CONFIRMADA) {
            log.info("Webhook Asaas duplicado para equipe '{}' (billingId={}) - ignorado",
                    team.getTeamName(), billingId);
            return ResponseEntity.ok().build();
        }

        team.setStatus(TeamStatus.CONFIRMADA);
        team.setConfirmedAt(Instant.now());
        teamRepository.save(team);
        log.info("Equipe '{}' CONFIRMADA via webhook Asaas (billingId={})", team.getTeamName(), billingId);
        return ResponseEntity.ok().build();
    }

    private Optional<Team> findTeam(String billingId, String externalId) {
        if (StringUtils.hasText(billingId)) {
            Optional<Team> byBilling = teamRepository.findByBillingId(billingId);
            if (byBilling.isPresent()) {
                return byBilling;
            }
        }
        if (StringUtils.hasText(externalId)) {
            try {
                return teamRepository.findById(UUID.fromString(externalId.trim()));
            } catch (IllegalArgumentException e) {
                log.warn("Webhook Asaas: externalReference nao e um UUID valido: {}", externalId);
            }
        }
        return Optional.empty();
    }

    private boolean isAuthentic(String asaasAccessToken, String secretHeader, String secretParam) {
        if (!StringUtils.hasText(webhookToken)) {
            log.error("asaas.webhook-token nao configurado - rejeitando webhook");
            return false;
        }
        return webhookToken.equals(asaasAccessToken)
                || webhookToken.equals(secretHeader)
                || webhookToken.equals(secretParam);
    }
}
