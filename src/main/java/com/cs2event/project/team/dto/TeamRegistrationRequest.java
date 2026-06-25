package com.cs2event.project.team.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TeamRegistrationRequest(
        @NotBlank
        @Size(max = 50)
        @Pattern(regexp = "^[\\p{L}\\p{N} ._'#-]+$", message = "use apenas letras, numeros, espacos e . _ ' # -")
        String teamName,

        @NotBlank
        @Size(max = 80)
        @Pattern(regexp = "^[\\p{L} .'-]+$", message = "use apenas letras, espacos e . ' -")
        String captainName,

        @NotBlank
        @Email
        @Size(max = 254)
        @Pattern(
                regexp = "^[A-Za-z0-9](?:[A-Za-z0-9_%+-]|\\.(?=[A-Za-z0-9_%+-])){0,63}@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$",
                message = "e-mail invalido"
        )
        String captainEmail,

        @NotBlank
        @Size(max = 40)
        @Pattern(regexp = "^[A-Za-z0-9._#-]+$", message = "Discord invalido")
        String captainDiscordId,

        @NotBlank
        @Size(max = 25)
        @Pattern(regexp = "^\\+?[0-9 ()-]{8,24}$", message = "WhatsApp invalido")
        String whatsapp,

        @NotEmpty
        @Size(max = 10)
        List<
                @NotBlank
                @Size(max = 40)
                @Pattern(regexp = "^[\\p{L}\\p{N} ._:/-]+$", message = "disponibilidade invalida")
                String> availability,

        @Size(max = 500)
        @Pattern(regexp = "^[\\p{L}\\p{N} .,:;!?/()@#%+_-]*$", message = "observacoes contem caracteres invalidos")
        String observations,

        @AssertTrue(message = "E necessario concordar com os termos")
        boolean termsAccepted
) {
}
