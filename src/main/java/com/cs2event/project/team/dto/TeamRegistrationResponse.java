package com.cs2event.project.team.dto;

import com.cs2event.project.team.Team;
import com.cs2event.project.team.TeamStatus;
import java.util.UUID;

/** Resposta de criação da inscrição (201). Não expõe dados sensíveis. */
public record TeamRegistrationResponse(UUID id, TeamStatus status) {

    public static TeamRegistrationResponse from(Team team) {
        return new TeamRegistrationResponse(team.getId(), team.getStatus());
    }
}
