package com.cs2event.project.team;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository extends JpaRepository<Team, UUID> {

    /** Usado pelo webhook para localizar a equipe pela cobrança paga. */
    Optional<Team> findByBillingId(String billingId);

    List<Team> findByStatusOrderByCreatedAtAsc(TeamStatus status);

    long countByStatus(TeamStatus status);
}
