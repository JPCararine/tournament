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
@RequestMapping("/webhooks/abacatepay")
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
        System.out.println(payload);
        if (!isAuthentic(secretParam, secretHeader)) {
            log.warn("Webhook AbacatePay rejeitado: secret inválido/ausente");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (payload == null) {
            log.warn("Webhook AbacatePay com corpo vazio");
            return ResponseEntity.badRequest().build();
        }

        String billingId = payload.resolveBillingId();
        String externalId = payload.resolveExternalId();
        Optional<Team> maybeTeam = findTeam(billingId, externalId);
        if (maybeTeam.isEmpty()) {
            log.warn("Webhook AbacatePay: nenhuma equipe para billingId={} externalId={}", billingId, externalId);
            return ResponseEntity.ok().build();
        }

        if (!payload.isPaid()) {
            log.info("Webhook AbacatePay ignorado (evento não pago): event={} billingId={}",
                    payload.event(), billingId);
            return ResponseEntity.ok().build();
        }

        Team team = maybeTeam.get();
        if (team.getStatus() == TeamStatus.CONFIRMADA) {
            log.info("Webhook AbacatePay duplicado para equipe '{}' (billingId={}) - ignorado",
                    team.getTeamName(), billingId);
            return ResponseEntity.ok().build();
        }

        team.setStatus(TeamStatus.CONFIRMADA);
        team.setConfirmedAt(Instant.now());
        teamRepository.save(team);
        log.info("Equipe '{}' CONFIRMADA via webhook (billingId={})", team.getTeamName(), billingId);
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
                log.warn("Webhook AbacatePay: externalId não é um UUID válido: {}", externalId);
            }
        }
        return Optional.empty();
    }

    private boolean isAuthentic(String secretParam, String secretHeader) {
        if (!StringUtils.hasText(webhookSecret)) {
            log.error("abacatepay.webhook-secret não configurado - rejeitando webhook");
            return false;
        }
        return webhookSecret.equals(secretParam) || webhookSecret.equals(secretHeader);
    }
}
