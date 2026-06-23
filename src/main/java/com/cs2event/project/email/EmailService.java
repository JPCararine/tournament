package com.cs2event.project.email;

import com.cs2event.project.payment.dto.PixCharge;
import com.cs2event.project.team.Team;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String TEMPLATE_PATH = "templates/email/confirmation.html";
    private static final String QR_CODE_CID = "pixQrCode";

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String discordInviteUrl;
    private final String championshipName;
    private final int maxTeams;
    private final String tournamentDate;
    private final int prizeBase;
    private final int prizePerTeam;
    private final String template;

    public EmailService(JavaMailSender mailSender,
                        @Value("${sendgrid.from-email:noreply@example.com}") String fromEmail,
                        @Value("${app.discord-invite-url:#}") String discordInviteUrl,
                        @Value("${app.tournament.name:Campeonato}") String championshipName,
                        @Value("${app.tournament.max-teams:16}") int maxTeams,
                        @Value("${app.tournament.date:A definir}") String tournamentDate,
                        @Value("${app.tournament.prize-base:0}") int prizeBase,
                        @Value("${app.tournament.prize-per-team:0}") int prizePerTeam) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.discordInviteUrl = discordInviteUrl;
        this.championshipName = championshipName;
        this.maxTeams = maxTeams;
        this.tournamentDate = tournamentDate;
        this.prizeBase = prizeBase;
        this.prizePerTeam = prizePerTeam;
        this.template = loadTemplate();
    }

    public void sendInviteAndCharge(Team team, PixCharge charge, long confirmedCount) {
        try {
            byte[] qrCodeBytes = QrCodeUtils.gerarQrCodePng(charge.brCode(), 300, 300);
            String html = buildHtml(charge, confirmedCount);
            String subject = "Inscrição recebida - " + championshipName + ": " + team.getTeamName();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail);
            helper.setTo(team.getCaptainEmail());
            helper.setSubject(subject);
            helper.setText(plainText(charge), html);
            helper.addInline(QR_CODE_CID, new ByteArrayResource(qrCodeBytes), "image/png");

            mailSender.send(message);
            log.info("E-mail de inscrição enviado para {} (equipe {})",
                    team.getCaptainEmail(), team.getTeamName());
        } catch (MessagingException | RuntimeException e) {
            log.error("Falha ao enviar e-mail de inscrição para equipe {} - registro mantido como PENDENTE",
                    team.getTeamName(), e);
        }
    }

    String buildHtml(PixCharge charge, long confirmedCount) {
        long prize = prizeBase + confirmedCount * prizePerTeam;
        return template
                .replace("__CHAMPIONSHIP__", escapeHtml(championshipName))
                .replace("__QR_CODE_SRC__", "cid:" + QR_CODE_CID)
                .replace("__VALUE__", escapeHtml(formatBrl(charge.amountCents())))
                .replace("__BR_CODE__", escapeHtml(Objects.toString(charge.brCode(), "")))
                .replace("__DISCORD_URL__", escapeHtmlAttribute(defaultUrl(discordInviteUrl)))
                .replace("__PAYMENT_HELP__", paymentHelpHtml(charge))
                .replace("__SLOTS__", escapeHtml(confirmedCount + "/" + maxTeams))
                .replace("__DATE__", escapeHtml(tournamentDate))
                .replace("__PRIZE__", escapeHtml(formatReais(prize)));
    }

    private String plainText(PixCharge charge) {
        return "PIX copia e cola: " + Objects.toString(charge.brCode(), "");
    }

    private String formatBrl(int amountCents) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("pt", "BR"));
        DecimalFormat df = new DecimalFormat("#,##0.00", symbols);
        return "R$ " + df.format(amountCents / 100.0);
    }

    private String formatReais(long reais) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("pt", "BR"));
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return "R$ " + df.format(reais);
    }

    private String paymentHelpHtml(PixCharge charge) {
        if (hasText(charge.url())) {
            return "Se o QR Code não carregar no seu aplicativo de e-mail, abra a cobrança diretamente: "
                    + "<a href=\"" + escapeHtmlAttribute(charge.url().trim())
                    + "\" target=\"_blank\" style=\"color:#93c5fd;text-decoration:underline;\">pagar via link</a>.";
        }
        return "Se o QR Code não carregar no seu aplicativo de e-mail, use o código PIX copia e cola exibido acima.";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String defaultUrl(String url) {
        return (url == null || url.isBlank()) ? "#" : url;
    }

    private String escapeHtml(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private String escapeHtmlAttribute(String value) {
        return escapeHtml(value);
    }

    private String loadTemplate() {
        try (var in = new ClassPathResource(TEMPLATE_PATH).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao carregar template de e-mail: " + TEMPLATE_PATH, e);
        }
    }
}
