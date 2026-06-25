package com.cs2event.project.team;

import com.cs2event.project.email.EmailService;
import com.cs2event.project.payment.PaymentAccessTokenService;
import com.cs2event.project.payment.PaymentService;
import com.cs2event.project.payment.dto.PixCharge;
import com.cs2event.project.team.dto.DashboardResponse;
import com.cs2event.project.team.dto.DashboardResponse.TeamSummary;
import com.cs2event.project.team.dto.TeamRegistrationRequest;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TeamService {

    private final TeamRepository teamRepository;
    private final PaymentService paymentService;
    private final PaymentAccessTokenService paymentAccessTokenService;
    private final EmailService emailService;
    private final PendingTeamCleanupService pendingTeamCleanupService;
    private final int maxTeams;
    private final int pixExpiresSeconds;

    public TeamService(TeamRepository teamRepository,
                       PaymentService paymentService,
                       PaymentAccessTokenService paymentAccessTokenService,
                       EmailService emailService,
                       PendingTeamCleanupService pendingTeamCleanupService,
                       @Value("${app.tournament.max-teams}") int maxTeams,
                       @Value("${asaas.pix-expires-seconds}") int pixExpiresSeconds) {
        this.teamRepository = teamRepository;
        this.paymentService = paymentService;
        this.paymentAccessTokenService = paymentAccessTokenService;
        this.emailService = emailService;
        this.pendingTeamCleanupService = pendingTeamCleanupService;
        this.maxTeams = maxTeams;
        this.pixExpiresSeconds = pixExpiresSeconds;
    }

    @Transactional
    public Team register(TeamRegistrationRequest request) {
        pendingTeamCleanupService.removeExpiredPendingTeams();

        String teamName = request.teamName().trim();
        String captainName = request.captainName().trim();
        String captainEmail = request.captainEmail().trim().toLowerCase(Locale.ROOT);
        String captainDiscordId = request.captainDiscordId().trim();
        String whatsapp = normalizeWhatsapp(request.whatsapp());
        List<String> availability = request.availability().stream()
                .map(String::trim)
                .toList();
        String observations = normalizeOptional(request.observations());

        Team team = new Team();
        long occupiedSlots = teamRepository.count();
        if (occupiedSlots >= maxTeams) {
            throw new RegistrationClosedException(
                    "O numero de inscritos chegou ao seu limite, agradecemos o seu interesse!");
        }
        validateNoDuplicate(teamName, captainEmail, captainDiscordId, whatsapp);
        team.setTeamName(teamName);
        team.setCaptainName(captainName);
        team.setCaptainEmail(captainEmail);
        team.setCaptainDiscordId(captainDiscordId);
        team.setWhatsapp(whatsapp);
        team.setAvailability(availability);
        team.setObservations(observations);
        team.setTermsAccepted(request.termsAccepted());
        team.setStatus(TeamStatus.PENDENTE);
        team.setCreatedAt(Instant.now());
        team = teamRepository.save(team);

        PixCharge charge = paymentService.createPixCharge(team);
        team.setBillingId(charge.billingId());
        team.setAmountCents(charge.amountCents());
        team.setPixQrCodeBase64(charge.brCodeBase64());
        team.setPixBrCode(charge.brCode());
        String paymentAccessToken = paymentAccessTokenService.generateToken();
        team.setPaymentAccessTokenHash(paymentAccessTokenService.hashToken(paymentAccessToken));
        team.setPaymentAccessExpiresAt(team.getCreatedAt().plusSeconds(pixExpiresSeconds));
        team = teamRepository.save(team);

        long confirmedCount = teamRepository.countByStatus(TeamStatus.CONFIRMADA);
        emailService.sendInviteAndCharge(team, paymentAccessToken, confirmedCount);

        return team;
    }

    private void validateNoDuplicate(String teamName,
                                     String captainEmail,
                                     String captainDiscordId,
                                     String whatsapp) {
        if (teamRepository.existsByTeamNameIgnoreCase(teamName)) {
            throw new DuplicateRegistrationException(
                    "Ja existe uma equipe inscrita com este nome.");
        }
        if (teamRepository.existsByCaptainEmailIgnoreCase(captainEmail)) {
            throw new DuplicateRegistrationException(
                    "Este e-mail ja foi utilizado em uma inscricao.");
        }
        if (teamRepository.existsByCaptainDiscordIdIgnoreCase(captainDiscordId)) {
            throw new DuplicateRegistrationException(
                    "Este Discord ja foi utilizado em uma inscricao.");
        }
        if (teamRepository.existsByWhatsapp(whatsapp)) {
            throw new DuplicateRegistrationException(
                    "Este WhatsApp ja foi utilizado em uma inscricao.");
        }
    }

    private String normalizeWhatsapp(String value) {
        return value.replaceAll("\\D", "");
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
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
