package com.cs2event.project.team.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TeamRegistrationRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void rejectsSqlInjectionPayloadInTeamName() {
        TeamRegistrationRequest request = validRequest("SG'; DROP TABLE team; --");

        assertThat(validator.validate(request))
                .anyMatch(violation -> "teamName".equals(violation.getPropertyPath().toString()));
    }

    @Test
    void acceptsExpectedRegistrationShape() {
        TeamRegistrationRequest request = validRequest("SG TEAM #1");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsMalformedEmail() {
        TeamRegistrationRequest request = new TeamRegistrationRequest(
                "SG TEAM",
                "Joao Pedro",
                "joao@localhost",
                "joao.pedro#0001",
                "+55 (11) 99999-8888",
                List.of("Sexta 20:00"),
                "Depois das 20h",
                true
        );

        assertThat(validator.validate(request))
                .anyMatch(violation -> "captainEmail".equals(violation.getPropertyPath().toString()));
    }

    private TeamRegistrationRequest validRequest(String teamName) {
        return new TeamRegistrationRequest(
                teamName,
                "Joao Pedro",
                "joao@example.com",
                "joao.pedro#0001",
                "+55 (11) 99999-8888",
                List.of("Sexta 20:00"),
                "Depois das 20h",
                true
        );
    }
}
