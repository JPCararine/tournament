package com.cs2event.project.payment;

import com.cs2event.project.payment.dto.PixCharge;
import com.cs2event.project.team.Team;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final AbacatePayClient abacatePayClient;
    private final int amountCents;

    public PaymentService(AbacatePayClient abacatePayClient,
                          @Value("${abacatepay.amount-cents}") int amountCents) {
        this.abacatePayClient = abacatePayClient;
        this.amountCents = amountCents;
    }

    public PixCharge createPixCharge(Team team) {
        String description = "Taxa de inscrição - Campeonato CS2 - " + team.getTeamName();
        return abacatePayClient.createPixCharge(team, amountCents, description);
    }
}
