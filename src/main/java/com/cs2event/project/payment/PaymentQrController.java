package com.cs2event.project.payment;

import com.cs2event.project.team.Team;
import com.cs2event.project.team.TeamRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentQrController {

    private static final Logger log = LoggerFactory.getLogger(PaymentQrController.class);
    private static final int QR_CODE_SIZE = 320;

    private final TeamRepository teamRepository;
    private final String discordInviteUrl;

    public PaymentQrController(TeamRepository teamRepository,
                               @Value("${app.discord-invite-url:#}") String discordInviteUrl) {
        this.teamRepository = teamRepository;
        this.discordInviteUrl = discordInviteUrl;
    }

    @GetMapping(value = "/{billingId}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> paymentPage(@PathVariable String billingId) {
        Optional<Team> maybeTeam = teamRepository.findByBillingId(billingId);
        if (maybeTeam.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Team team = maybeTeam.get();
        String qrCodeSrc = qrCodeSrc(team, billingId);
        String html = """
                <!DOCTYPE html>
                <html lang="pt-BR">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1.0">
                    <title>Pagamento Pix</title>
                    <style>
                        body{margin:0;font-family:Arial,Helvetica,sans-serif;background:#0b1220;color:#e5e7eb}
                        main{max-width:760px;margin:0 auto;padding:32px 18px}
                        section{background:#111827;border:1px solid #263041;border-radius:12px;padding:28px}
                        h1{margin:0 0 8px;font-size:30px;line-height:36px}
                        p{line-height:1.55;color:#cbd5e1}
                        .qr{display:block;width:260px;height:260px;margin:24px auto;padding:14px;background:white;border-radius:8px}
                        .code{background:#0f172a;border:1px solid #243041;border-radius:8px;padding:14px;word-break:break-all;color:#f8fafc}
                        .actions{display:flex;gap:12px;flex-wrap:wrap;margin-top:22px}
                        a,.copy{border:0;border-radius:8px;padding:13px 16px;font-weight:700;text-decoration:none;cursor:pointer}
                        a{background:#5865f2;color:white}
                        .copy{background:#34d399;color:#052e22}
                        .meta{font-size:13px;text-transform:uppercase;letter-spacing:1px;color:#93c5fd;font-weight:700}
                    </style>
                </head>
                <body>
                <main>
                    <section>
                        <p class="meta">Inscricao pendente</p>
                        <h1>Pagamento Pix</h1>
                        <p>Equipe: <strong>__TEAM_NAME__</strong></p>
                        <p>Use o QR Code abaixo ou copie o codigo Pix. Depois entre no Discord para acompanhar o torneio.</p>
                        <img class="qr" src="__QR_CODE_SRC__" alt="QR Code Pix para pagamento">
                        <p class="meta">Pix copia e cola</p>
                        <div class="code" id="pixCode">__BR_CODE__</div>
                        <div class="actions">
                            <button class="copy" type="button" onclick="navigator.clipboard.writeText(document.getElementById('pixCode').innerText)">Copiar Pix</button>
                            <a href="__DISCORD_URL__" target="_blank" rel="noopener">Entrar no Discord</a>
                        </div>
                    </section>
                </main>
                </body>
                </html>
                """
                .replace("__TEAM_NAME__", escapeHtml(team.getTeamName()))
                .replace("__QR_CODE_SRC__", escapeHtmlAttribute(qrCodeSrc))
                .replace("__BR_CODE__", escapeHtml(team.getPixBrCode()))
                .replace("__DISCORD_URL__", escapeHtmlAttribute(defaultUrl(discordInviteUrl)));

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(html);
    }

    @GetMapping("/{billingId}/qr.png")
    public ResponseEntity<byte[]> qrCode(@PathVariable String billingId) {
        Optional<Team> maybeTeam = teamRepository.findByBillingId(billingId);
        if (maybeTeam.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        byte[] png = generateQrCode(maybeTeam.get().getPixBrCode());
        if (png.length == 0) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .cacheControl(CacheControl.noCache())
                .body(png);
    }

    private byte[] generateQrCode(String brCode) {
        if (!StringUtils.hasText(brCode)) {
            return new byte[0];
        }

        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(
                    brCode.trim(),
                    BarcodeFormat.QR_CODE,
                    QR_CODE_SIZE,
                    QR_CODE_SIZE,
                    Map.of(EncodeHintType.MARGIN, 1)
            );

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", output);
            return output.toByteArray();
        } catch (WriterException | IOException e) {
            log.error("Falha ao gerar QR Code PIX para o brCode informado", e);
            return new byte[0];
        }
    }

    private String qrCodeSrc(Team team, String billingId) {
        if (StringUtils.hasText(team.getPixQrCodeBase64())) {
            String encodedImage = team.getPixQrCodeBase64().trim();
            if (encodedImage.startsWith("data:image/")) {
                return encodedImage;
            }
            return "data:image/png;base64," + encodedImage;
        }
        return "/api/payments/" + billingId + "/qr.png";
    }

    private String defaultUrl(String url) {
        return StringUtils.hasText(url) ? url.trim() : "#";
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
}
