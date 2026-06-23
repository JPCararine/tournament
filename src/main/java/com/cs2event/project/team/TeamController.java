package com.cs2event.project.team;

import com.cs2event.project.team.dto.DashboardResponse;
import com.cs2event.project.team.dto.TeamRegistrationRequest;
import com.cs2event.project.team.dto.TeamRegistrationResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamRegistrationResponse> register(
            @Valid @RequestBody TeamRegistrationRequest request) {
        Team team = teamService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(TeamRegistrationResponse.from(team));
    }

    @GetMapping
    public DashboardResponse dashboard() {
        return teamService.dashboard();
    }
}
