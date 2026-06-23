package com.cs2event.project.team;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Remove automaticamente as equipes que continuam PENDENTES após o prazo de
 * expiração da cobrança Pix (o mesmo {@code expiresIn} enviado ao AbacatePay e
 * exibido no e-mail). Equipes CONFIRMADA nunca são removidas — ficam
 * persistidas para sempre.
 */
@Service
public class PendingTeamCleanupService {

    private static final Logger log = LoggerFactory.getLogger(PendingTeamCleanupService.class);

    private final TeamRepository teamRepository;
    private final int pixExpiresSeconds;

    public PendingTeamCleanupService(TeamRepository teamRepository,
                                     @Value("${abacatepay.pix-expires-seconds}") int pixExpiresSeconds) {
        this.teamRepository = teamRepository;
        this.pixExpiresSeconds = pixExpiresSeconds;
    }

    @Scheduled(fixedDelayString = "${app.pending-cleanup.interval-ms:60000}")
    @Transactional
    public void removeExpiredPendingTeams() {
        Instant threshold = Instant.now().minusSeconds(pixExpiresSeconds);
        List<Team> expired = teamRepository.findByStatusAndCreatedAtBefore(TeamStatus.PENDENTE, threshold);
        if (expired.isEmpty()) {
            return;
        }
        teamRepository.deleteAll(expired);
        log.info("Removidas {} equipe(s) pendente(s) expirada(s) (criadas antes de {})",
                expired.size(), threshold);
    }
}
