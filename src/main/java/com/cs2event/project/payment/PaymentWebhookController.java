package com.cs2event.project.payment;

import com.cs2event.project.payment.dto.WebhookPayload;
import com.cs2event.project.team.Team;
import com.cs2event.project.team.TeamRepository;
import com.cs2event.project.team.TeamStatus;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
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
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (payload == null) {
            return ResponseEntity.badRequest().build();
        }

        String billingId = payload.resolveBillingId();
        String externalId = payload.resolveExternalId();
        Optional<Team> maybeTeam = findTeam(billingId, externalId);
        if (maybeTeam.isEmpty()) {
            return ResponseEntity.ok().build();
        }

        if (!payload.isPaid()) {
            return ResponseEntity.ok().build();
        }

        Team team = maybeTeam.get();
        if (team.getStatus() == TeamStatus.CONFIRMADA) {
            return ResponseEntity.ok().build();
        }

        team.setStatus(TeamStatus.CONFIRMADA);
        team.setConfirmedAt(Instant.now());
        teamRepository.save(team);
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
            }
        }
        return Optional.empty();
    }

    private boolean isAuthentic(String asaasAccessToken, String secretHeader, String secretParam) {
        if (!StringUtils.hasText(webhookToken)) {
            return false;
        }
        return webhookToken.equals(asaasAccessToken)
                || webhookToken.equals(secretHeader)
                || webhookToken.equals(secretParam);
    }
}
