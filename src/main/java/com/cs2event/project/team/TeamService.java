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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

    private static final Logger log = LoggerFactory.getLogger(TeamService.class);

    private final TeamRepository teamRepository;
    private final PaymentService paymentService;
    private final EmailService emailService;
    private final int maxTeams;

    public TeamService(TeamRepository teamRepository,
                       PaymentService paymentService,
                       EmailService emailService,
                       @Value("${app.tournament.max-teams}") int maxTeams) {
        this.teamRepository = teamRepository;
        this.paymentService = paymentService;
        this.emailService = emailService;
        this.maxTeams = maxTeams;
    }

    @Transactional
    public Team register(TeamRegistrationRequest request) {
        Team team = new Team();
        // Cada equipe — pendente ou confirmada — ocupa uma vaga. Ao atingir o
        // limite, as inscrições ficam travadas. Pendentes não pagas expiram e
        // liberam a vaga automaticamente, reabrindo o sistema.
        long occupiedSlots = teamRepository.count();
        if (occupiedSlots >= maxTeams) {
            throw new RegistrationClosedException(
                    "O número de inscritos chegou ao seu limite, agradecemos o seu interesse!");
        }
        validateNoDuplicate(request);
        team.setTeamName(request.teamName().trim());
        team.setCaptainName(request.captainName().trim());
        team.setCaptainEmail(request.captainEmail().trim());
        team.setCaptainDiscordId(request.captainDiscordId().trim());
        team.setWhatsapp(request.whatsapp().trim());
        team.setAvailability(request.availability());
        team.setObservations(request.observations());
        team.setTermsAccepted(request.termsAccepted());
        team.setStatus(TeamStatus.PENDENTE);
        team.setCreatedAt(Instant.now());
        team = teamRepository.save(team);

        PixCharge charge = paymentService.createPixCharge(team);
        team.setBillingId(charge.billingId());
        team.setAmountCents(charge.amountCents());
        team.setPixQrCodeBase64(charge.brCodeBase64());
        team.setPixBrCode(charge.brCode());
        team = teamRepository.save(team);

        long confirmedCount = teamRepository.countByStatus(TeamStatus.CONFIRMADA);
        emailService.sendInviteAndCharge(team, charge, confirmedCount);

        log.info("Equipe '{}' registrada como PENDENTE (id={}, billingId={})",
                team.getTeamName(), team.getId(), team.getBillingId());
        return team;
    }

    /**
     * Garante que nenhum dado identificador da inscrição já esteja em uso por
     * outra equipe — pendente ou confirmada. Equipes pendentes que expiraram
     * são removidas automaticamente, liberando o dado para uma nova inscrição.
     */
    private void validateNoDuplicate(TeamRegistrationRequest request) {
        if (teamRepository.existsByTeamNameIgnoreCase(request.teamName().trim())) {
            throw new DuplicateRegistrationException(
                    "Já existe uma equipe inscrita com este nome.");
        }
        if (teamRepository.existsByCaptainEmailIgnoreCase(request.captainEmail().trim())) {
            throw new DuplicateRegistrationException(
                    "Este e-mail já foi utilizado em uma inscrição.");
        }
        if (teamRepository.existsByCaptainDiscordIdIgnoreCase(request.captainDiscordId().trim())) {
            throw new DuplicateRegistrationException(
                    "Este Discord já foi utilizado em uma inscrição.");
        }
        if (teamRepository.existsByWhatsapp(request.whatsapp().trim())) {
            throw new DuplicateRegistrationException(
                    "Este WhatsApp já foi utilizado em uma inscrição.");
        }
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard() {
        List<TeamSummary> pendentes = teamRepository
                .findByStatusOrderByCreatedAtAsc(TeamStatus.PENDENTE)
                .stream().map(TeamSummary::from).toList();
        List<TeamSummary> confirmadas = teamRepository
                .findByStatusOrderByCreatedAtAsc(TeamStatus.CONFIRMADA)
                .stream().map(TeamSummary::from).toList();
        long occupiedSlots = (long) pendentes.size() + confirmadas.size();
        boolean registrationOpen = occupiedSlots < maxTeams;
        return new DashboardResponse(pendentes, confirmadas,
                pendentes.size(), confirmadas.size(), maxTeams, registrationOpen);
    }
}
