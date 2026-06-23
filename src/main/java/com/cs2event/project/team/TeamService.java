package com.cs2event.project.team;

import com.cs2event.project.email.EmailService;
import com.cs2event.project.payment.PaymentService;
import com.cs2event.project.payment.dto.PixCharge;
import com.cs2event.project.team.dto.DashboardResponse;
import com.cs2event.project.team.dto.DashboardResponse.TeamSummary;
import com.cs2event.project.team.dto.TeamRegistrationRequest;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestra o ciclo de inscrição: persiste PENDENTE → cria cobrança → envia e-mail,
 * e expõe a leitura pública do dashboard.
 */
@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final PaymentService paymentService;
    private final EmailService emailService;

    public TeamService(TeamRepository teamRepository,
                       PaymentService paymentService,
                       EmailService emailService) {
        this.teamRepository = teamRepository;
        this.paymentService = paymentService;
        this.emailService = emailService;
    }

    /**
     * Registra a manifestação de interesse (Fluxo A).
     *
     * <p>A equipe é gravada como PENDENTE primeiro; em seguida cria-se a cobrança
     * Pix e dispara-se o e-mail. Falha de e-mail não desfaz a inscrição.</p>
     */
    @Transactional
    public Team register(TeamRegistrationRequest request) {
        Team team = new Team();
        team.setTeamName(request.teamName());
        team.setCaptainName(request.captainName());
        team.setCaptainEmail(request.captainEmail());
        team.setCaptainDiscordId(request.captainDiscordId());
        team.setWhatsapp(request.whatsapp());
        team.setAvailability(request.availability());
        team.setObservations(request.observations());
        team.setTermsAccepted(request.termsAccepted());
        team.setStatus(TeamStatus.PENDENTE);
        team.setCreatedAt(Instant.now());
        team = teamRepository.save(team);

        PixCharge charge = paymentService.createPixCharge(team);
        team.setBillingId(charge.billingId());
        team.setAmountCents(charge.amountCents());
        team = teamRepository.save(team);

        // Falha de e-mail é tratada internamente (logada) e não aborta a inscrição.
        emailService.sendInviteAndCharge(team, charge);

        log.info("Equipe '{}' registrada como PENDENTE (id={}, billingId={})",
                team.getTeamName(), team.getId(), team.getBillingId());
        return team;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        List<TeamSummary> pendentes = teamRepository
                .findByStatusOrderByCreatedAtAsc(TeamStatus.PENDENTE)
                .stream().map(TeamSummary::from).toList();
        List<TeamSummary> confirmadas = teamRepository
                .findByStatusOrderByCreatedAtAsc(TeamStatus.CONFIRMADA)
                .stream().map(TeamSummary::from).toList();
        return new DashboardResponse(pendentes, confirmadas, pendentes.size(), confirmadas.size());
    }
}
