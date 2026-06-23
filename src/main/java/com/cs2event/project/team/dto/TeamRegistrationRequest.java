package com.cs2event.project.team.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

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
