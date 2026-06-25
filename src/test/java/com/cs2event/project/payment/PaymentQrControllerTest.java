package com.cs2event.project.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cs2event.project.team.Team;
import com.cs2event.project.team.TeamRepository;
import com.cs2event.project.team.TeamStatus;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class PaymentQrControllerTest {

    @Mock
    private TeamRepository teamRepository;

    private final PaymentAccessTokenService tokenService = new PaymentAccessTokenService();

    @Test
    void resolvesPaymentDetailsByAccessTokenHash() {
        String accessToken = tokenService.generateToken();
        String tokenHash = tokenService.hashToken(accessToken);
        Team team = new Team();
        team.setStatus(TeamStatus.PENDENTE);
        team.setPixBrCode("000201-test");
        team.setTeamName("SG TEAM");
        when(teamRepository.findByPaymentAccessTokenHashAndPaymentAccessExpiresAtAfter(
                eq(tokenHash), any(Instant.class))).thenReturn(Optional.of(team));
        PaymentQrController controller = new PaymentQrController(
                teamRepository, tokenService, "https://discord.gg/test");

        ResponseEntity<PaymentQrController.PaymentDetailsResponse> response =
                controller.paymentDetails(accessToken);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().copyPaste()).isEqualTo("000201-test");
        assertThat(response.getBody().teamName()).isEqualTo("SG TEAM");
        verify(teamRepository).findByPaymentAccessTokenHashAndPaymentAccessExpiresAtAfter(
                eq(tokenHash), any(Instant.class));
    }

    @Test
    void returnsNotFoundForExpiredOrUnknownAccessToken() {
        String accessToken = tokenService.generateToken();
        PaymentQrController controller = new PaymentQrController(
                teamRepository, tokenService, "https://discord.gg/test");

        ResponseEntity<PaymentQrController.PaymentDetailsResponse> response =
                controller.paymentDetails(accessToken);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }
}
