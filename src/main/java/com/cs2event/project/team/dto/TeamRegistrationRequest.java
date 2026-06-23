package com.cs2event.project.team.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Payload de inscrição enviado pelo front (Fluxo A).
 *
 * <p>Validação via Bean Validation: campos obrigatórios, e-mail válido,
 * disponibilidade não vazia e aceite obrigatório dos termos.</p>
 */
public record TeamRegistrationRequest(
        @NotBlank String teamName,
        @NotBlank String captainName,
        @NotBlank @Email String captainEmail,
        @NotBlank String captainDiscordId,
        @NotBlank String whatsapp,
        @NotEmpty List<@NotBlank String> availability,
        String observations,
        @AssertTrue(message = "É necessário concordar com os termos") boolean termsAccepted
) {
}
