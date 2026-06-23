package com.cs2event.project.payment;

import com.cs2event.project.payment.dto.PixCharge;
import com.cs2event.project.team.Team;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private static final int ASAAS_PIX_DESCRIPTION_MAX_LENGTH = 37;
    private static final String PIX_DESCRIPTION_PREFIX = "Inscricao CS2 - ";

    private final AsaasClient asaasClient;
    private final int amountCents;

    public PaymentService(AsaasClient asaasClient,
                          @Value("${asaas.amount-cents}") int amountCents) {
        this.asaasClient = asaasClient;
        this.amountCents = amountCents;
    }

    public PixCharge createPixCharge(Team team) {
        String description = truncateDescription(PIX_DESCRIPTION_PREFIX + team.getTeamName());
        return asaasClient.createPixCharge(team, amountCents, description);
    }

    private String truncateDescription(String description) {
        if (description.length() <= ASAAS_PIX_DESCRIPTION_MAX_LENGTH) {
            return description;
        }
        return description.substring(0, ASAAS_PIX_DESCRIPTION_MAX_LENGTH);
    }
}
