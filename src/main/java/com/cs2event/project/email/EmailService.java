package com.cs2event.project.email;

import com.cs2event.project.payment.dto.PixCharge;
import com.cs2event.project.team.Team;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Envio transacional de e-mail via SendGrid.
 *
 * <p>Desacoplado do gateway: recebe os dados da cobrança já prontos
 * ({@link PixCharge}) e não conhece o AbacatePay.</p>
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final SendGrid sendGrid;
    private final String fromEmail;
    private final String discordInviteUrl;

    public EmailService(SendGrid sendGrid,
                        @Value("${sendgrid.from-email}") String fromEmail,
                        @Value("${app.discord-invite-url}") String discordInviteUrl) {
        this.sendGrid = sendGrid;
        this.fromEmail = fromEmail;
        this.discordInviteUrl = discordInviteUrl;
    }

    /**
     * Envia ao capitão o e-mail com link do Discord + dados da cobrança Pix.
     *
     * <p>Falha de envio é logada e NÃO propagada: a inscrição já está persistida
     * como PENDENTE e não pode ser perdida por causa do e-mail.</p>
     */
    public void sendInviteAndCharge(Team team, PixCharge charge) {
        try {
            Email from = new Email(fromEmail);
            Email to = new Email(team.getCaptainEmail());
            String subject = "Inscrição recebida — Campeonato CS2: " + team.getTeamName();
            Content content = new Content("text/html", buildHtml(team, charge));

            Mail mail = new Mail(from, subject, to, content);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            if (response.getStatusCode() >= 400) {
                log.error("SendGrid retornou {} ao enviar e-mail para equipe {}: {}",
                        response.getStatusCode(), team.getTeamName(), response.getBody());
            } else {
                log.info("E-mail de inscrição enviado para {} (equipe {})",
                        team.getCaptainEmail(), team.getTeamName());
            }
        } catch (IOException | RuntimeException e) {
            log.error("Falha ao enviar e-mail de inscrição para equipe {} — registro mantido como PENDENTE",
                    team.getTeamName(), e);
        }
    }

    private String buildHtml(Team team, PixCharge charge) {
        String qrImg = charge.brCodeBase64() == null ? ""
                : "<p><img alt=\"QR-code Pix\" src=\"" + asDataUri(charge.brCodeBase64()) + "\"/></p>";
        String link = charge.url() == null ? ""
                : "<p>Ou pague pelo link: <a href=\"" + charge.url() + "\">" + charge.url() + "</a></p>";

        return """
                <h2>Inscrição recebida — Campeonato CS2</h2>
                <p>Olá, %s! Recebemos a manifestação de interesse da equipe <b>%s</b>.</p>
                <p><b>Atenção:</b> a vaga só é confirmada após o pagamento da taxa de inscrição.
                A ordem de confirmação segue a ordem dos pagamentos recebidos.</p>
                <h3>1) Entre no Discord da organização</h3>
                <p><a href="%s">%s</a></p>
                <h3>2) Pague a taxa de inscrição via Pix</h3>
                %s
                <p>Pix copia-e-cola:</p>
                <pre style="white-space:pre-wrap;word-break:break-all;">%s</pre>
                %s
                <p>Após o pagamento, sua equipe passará automaticamente para "CONFIRMADA".</p>
                """.formatted(
                team.getCaptainName(),
                team.getTeamName(),
                discordInviteUrl,
                discordInviteUrl,
                qrImg,
                charge.brCode() == null ? "" : charge.brCode(),
                link
        );
    }

    private String asDataUri(String base64) {
        return base64.startsWith("data:") ? base64 : "data:image/png;base64," + base64;
    }
}
