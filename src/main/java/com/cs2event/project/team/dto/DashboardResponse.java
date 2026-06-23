package com.cs2event.project.team.dto;

import com.cs2event.project.team.Team;
import com.cs2event.project.team.TeamStatus;
import java.util.List;

public record DashboardResponse(
        List<TeamSummary> pendentes,
        List<TeamSummary> confirmadas,
        long totalPendentes,
        long totalConfirmadas,
        long maxTeams,
        boolean registrationOpen
) {

    public record TeamSummary(String teamName, TeamStatus status) {
        public static TeamSummary from(Team team) {
            return new TeamSummary(team.getTeamName(), team.getStatus());
        }
    }
}
