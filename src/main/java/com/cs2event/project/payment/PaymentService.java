package com.cs2event.project.payment;

import com.cs2event.project.payment.dto.PixCharge;
import com.cs2event.project.team.Team;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final AsaasClient asaasClient;
    private final int amountCents;

    public PaymentService(AsaasClient asaasClient,
                          @Value("${asaas.amount-cents}") int amountCents) {
        this.asaasClient = asaasClient;
        this.amountCents = amountCents;
    }

    public PixCharge createPixCharge(Team team) {
        String description = "Taxa de inscrição - Campeonato CS2 - " + team.getTeamName();
        return asaasClient.createPixCharge(team, amountCents, description);
    }
}
