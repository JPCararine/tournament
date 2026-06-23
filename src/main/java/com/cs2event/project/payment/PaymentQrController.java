package com.cs2event.project.payment;

import com.cs2event.project.team.Team;
import com.cs2event.project.team.TeamRepository;
import com.cs2event.project.team.TeamStatus;
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

    @GetMapping(value = "/{billingId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentDetailsResponse> paymentDetails(@PathVariable String billingId) {
        Optional<Team> maybeTeam = teamRepository.findByBillingId(billingId);
        if (maybeTeam.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Team team = maybeTeam.get();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noCache())
                .body(new PaymentDetailsResponse(
                        team.getStatus(),
                        team.getStatus() == TeamStatus.CONFIRMADA ? null : team.getPixBrCode(),
                        defaultUrl(discordInviteUrl),
                        team.getTeamName()
                ));
    }

    @GetMapping("/{billingId}/qr.png")
    public ResponseEntity<byte[]> qrCode(@PathVariable String billingId) {
        Optional<Team> maybeTeam = teamRepository.findByBillingId(billingId);
        if (maybeTeam.isEmpty() || maybeTeam.get().getStatus() == TeamStatus.CONFIRMADA) {
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

    private String defaultUrl(String url) {
        return StringUtils.hasText(url) ? url.trim() : "#";
    }

    public record PaymentDetailsResponse(
            TeamStatus status,
            String copyPaste,
            String discordUrl,
            String teamName
    ) {
    }
}
