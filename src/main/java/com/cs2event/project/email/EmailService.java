package com.cs2event.project.email;

import com.cs2event.project.team.Team;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final String fromEmail;
    private final String publicBaseUrl;
    private final String championshipName;
    private final int maxTeams;
    private final String tournamentDate;
    private final int prizeBase;
    private final int prizePerTeam;

    public EmailService(JavaMailSender mailSender,
                        @Value("${sendgrid.from-email:noreply@example.com}") String fromEmail,
                        @Value("${app.public-base-url:http://localhost:8080}") String publicBaseUrl,
                        @Value("${app.tournament.name:Campeonato}") String championshipName,
                        @Value("${app.tournament.max-teams:16}") int maxTeams,
                        @Value("${app.tournament.date:A definir}") String tournamentDate,
                        @Value("${app.tournament.prize-base:0}") int prizeBase,
                        @Value("${app.tournament.prize-per-team:0}") int prizePerTeam) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
        this.publicBaseUrl = publicBaseUrl;
        this.championshipName = championshipName;
        this.maxTeams = maxTeams;
        this.tournamentDate = tournamentDate;
        this.prizeBase = prizeBase;
        this.prizePerTeam = prizePerTeam;
    }

    public void sendInviteAndCharge(Team team, long confirmedCount) {
        try {
            String paymentUrl = paymentUrl(team);
            String html = buildHtml(team, paymentUrl, confirmedCount);
            String subject = "Inscricao recebida - " + championshipName + ": " + team.getTeamName();

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(
                    message,
                    MimeMessageHelper.MULTIPART_MODE_MIXED_RELATED,
                    StandardCharsets.UTF_8.name()
            );

            helper.setFrom(fromEmail);
            helper.setTo(team.getCaptainEmail());
            helper.setSubject(subject);
            helper.setText(plainText(paymentUrl), html);

            mailSender.send(message);
            log.info("E-mail de inscricao enviado para {} (equipe {})",
                    team.getCaptainEmail(), team.getTeamName());
        } catch (MessagingException | RuntimeException e) {
            log.error("Falha ao enviar e-mail de inscricao para equipe {} - registro mantido como PENDENTE",
                    team.getTeamName(), e);
        }
    }

    String buildHtml(Team team, String paymentUrl, long confirmedCount) {
        long prize = prizeBase + confirmedCount * prizePerTeam;
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Pagamento da inscricao - __CHAMPIONSHIP__</title>
                </head>
                <body style="margin:0;padding:0;background-color:#0b1220;font-family:Arial,Helvetica,sans-serif;color:#e5e7eb;">
                <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="background-color:#0b1220;margin:0;padding:24px 0;">
                    <tr>
                        <td align="center">
                            <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="max-width:620px;background-color:#111827;border:1px solid #263041;border-radius:12px;overflow:hidden;">
                                <tr>
                                    <td style="padding:32px;">
                                        <p style="margin:0 0 12px 0;font-size:12px;line-height:18px;letter-spacing:1.4px;font-weight:700;color:#34d399;text-transform:uppercase;">Inscricao pendente</p>
                                        <h1 style="margin:0;font-size:30px;line-height:36px;font-weight:800;color:#f9fafb;">Finalize o pagamento Pix</h1>
                                        <p style="margin:14px 0 0 0;font-size:16px;line-height:26px;color:#cbd5e1;">Sua equipe <strong style="color:#f9fafb;">__TEAM_NAME__</strong> foi registrada. A vaga sera confirmada automaticamente apos o pagamento.</p>
                                        <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="margin:24px 0;">
                                            <tr>
                                                <td align="center" bgcolor="#34d399" style="border-radius:8px;">
                                                    <a href="__PAYMENT_URL__" target="_blank" style="display:block;padding:15px 20px;font-size:14px;line-height:20px;font-weight:700;letter-spacing:1px;text-transform:uppercase;color:#052e22;text-decoration:none;">Abrir pagamento</a>
                                                </td>
                                            </tr>
                                        </table>
                                        <p style="margin:0;font-size:14px;line-height:22px;color:#94a3b8;">O link contem o QR Code Pix, o codigo copia e cola e o acesso ao Discord.</p>
                                    </td>
                                </tr>
                                <tr>
                                    <td style="padding:0 32px 32px 32px;">
                                        <table role="presentation" cellpadding="0" cellspacing="0" border="0" width="100%" style="border-top:1px solid #243041;padding-top:20px;">
                                            <tr>
                                                <td width="33.33%" valign="top" style="padding-top:20px;">
                                                    <p style="margin:0;font-size:12px;line-height:18px;font-weight:700;letter-spacing:1.4px;text-transform:uppercase;color:#93c5fd;">Slots</p>
                                                    <p style="margin:8px 0 0 0;font-size:20px;line-height:26px;font-weight:700;color:#f9fafb;">__SLOTS__</p>
                                                </td>
                                                <td width="33.33%" valign="top" style="padding-top:20px;">
                                                    <p style="margin:0;font-size:12px;line-height:18px;font-weight:700;letter-spacing:1.4px;text-transform:uppercase;color:#93c5fd;">Data</p>
                                                    <p style="margin:8px 0 0 0;font-size:20px;line-height:26px;font-weight:700;color:#f9fafb;">__DATE__</p>
                                                </td>
                                                <td width="33.33%" valign="top" style="padding-top:20px;">
                                                    <p style="margin:0;font-size:12px;line-height:18px;font-weight:700;letter-spacing:1.4px;text-transform:uppercase;color:#93c5fd;">Prize</p>
                                                    <p style="margin:8px 0 0 0;font-size:20px;line-height:26px;font-weight:700;color:#f9fafb;">__PRIZE__</p>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
                </body>
                </html>
                """
                .replace("__CHAMPIONSHIP__", escapeHtml(championshipName))
                .replace("__TEAM_NAME__", escapeHtml(team.getTeamName()))
                .replace("__PAYMENT_URL__", escapeHtmlAttribute(paymentUrl))
                .replace("__SLOTS__", escapeHtml(confirmedCount + "/" + maxTeams))
                .replace("__DATE__", escapeHtml(tournamentDate))
                .replace("__PRIZE__", escapeHtml(formatReais(prize)));
    }

    private String plainText(String paymentUrl) {
        return "Para concluir sua inscricao, acesse o link de pagamento: " + paymentUrl;
    }

    private String paymentUrl(Team team) {
        return trimTrailingSlash(publicBaseUrl) + "/api/payments/" + team.getBillingId();
    }

    private String formatReais(long reais) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("pt", "BR"));
        DecimalFormat df = new DecimalFormat("#,##0", symbols);
        return "R$ " + df.format(reais);
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

    private String trimTrailingSlash(String value) {
        String baseUrl = (value == null || value.isBlank()) ? "http://localhost:8080" : value.trim();
        while (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
