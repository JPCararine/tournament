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
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
@Validated
public class PaymentQrController {

    private static final int QR_CODE_SIZE = 320;
    private static final int ACCESS_TOKEN_LENGTH = 43;
    private static final String ACCESS_TOKEN_PATTERN = "^[A-Za-z0-9_-]+$";

    private final TeamRepository teamRepository;
    private final PaymentAccessTokenService paymentAccessTokenService;
    private final String discordInviteUrl;

    public PaymentQrController(TeamRepository teamRepository,
                               PaymentAccessTokenService paymentAccessTokenService,
                               @Value("${app.discord-invite-url:#}") String discordInviteUrl) {
        this.teamRepository = teamRepository;
        this.paymentAccessTokenService = paymentAccessTokenService;
        this.discordInviteUrl = discordInviteUrl;
    }

    @GetMapping(value = "/{accessToken}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PaymentDetailsResponse> paymentDetails(
            @PathVariable
            @Size(min = ACCESS_TOKEN_LENGTH, max = ACCESS_TOKEN_LENGTH, message = "token de pagamento invalido")
            @Pattern(regexp = ACCESS_TOKEN_PATTERN, message = "token de pagamento invalido")
            String accessToken) {
        Optional<Team> maybeTeam = findTeamByAccessToken(accessToken);
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

    @GetMapping("/{accessToken}/qr.png")
    public ResponseEntity<byte[]> qrCode(
            @PathVariable
            @Size(min = ACCESS_TOKEN_LENGTH, max = ACCESS_TOKEN_LENGTH, message = "token de pagamento invalido")
            @Pattern(regexp = ACCESS_TOKEN_PATTERN, message = "token de pagamento invalido")
            String accessToken) {
        Optional<Team> maybeTeam = findTeamByAccessToken(accessToken);
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

    private Optional<Team> findTeamByAccessToken(String accessToken) {
        String tokenHash = paymentAccessTokenService.hashToken(accessToken);
        return teamRepository.findByPaymentAccessTokenHashAndPaymentAccessExpiresAtAfter(
                tokenHash, Instant.now());
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
