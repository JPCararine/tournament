package com.cs2event.project.team;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    Optional<Team> findByBillingId(String billingId);

    Optional<Team> findByPaymentAccessTokenHashAndPaymentAccessExpiresAtAfter(
            String paymentAccessTokenHash, Instant now);

    List<Team> findByStatusOrderByCreatedAtAsc(TeamStatus status);

    long countByStatus(TeamStatus status);

    boolean existsByTeamNameIgnoreCase(String teamName);

    boolean existsByCaptainEmailIgnoreCase(String captainEmail);

    boolean existsByCaptainDiscordIdIgnoreCase(String captainDiscordId);

    boolean existsByWhatsapp(String whatsapp);

    List<Team> findByStatusAndCreatedAtBefore(TeamStatus status, Instant threshold);
}
