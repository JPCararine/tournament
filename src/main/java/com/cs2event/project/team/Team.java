package com.cs2event.project.team;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "team", uniqueConstraints = {
        @UniqueConstraint(name = "uk_team_name", columnNames = "team_name"),
        @UniqueConstraint(name = "uk_team_captain_email", columnNames = "captain_email"),
        @UniqueConstraint(name = "uk_team_captain_discord_id", columnNames = "captain_discord_id"),
        @UniqueConstraint(name = "uk_team_whatsapp", columnNames = "whatsapp")
})
@Getter
@Setter
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "team_name", nullable = false)
    private String teamName;

    @Column(nullable = false)
    private String captainName;

    @Column(name = "captain_email", nullable = false)
    private String captainEmail;

    @Column(name = "captain_discord_id", nullable = false)
    private String captainDiscordId;

    @Column(name = "whatsapp", nullable = false)
    private String whatsapp;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "team_availability", joinColumns = @JoinColumn(name = "team_id"))
    @Column(name = "slot")
    private List<String> availability = new ArrayList<>();

    @Column(columnDefinition = "text")
    private String observations;

    @Column(nullable = false)
    private boolean termsAccepted;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TeamStatus status = TeamStatus.PENDENTE;

    @Column(unique = true)
    private String billingId;

    @Column(columnDefinition = "text")
    private String pixQrCodeBase64;

    @Column(columnDefinition = "text")
    private String pixBrCode;

    private Integer amountCents;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant confirmedAt;
}
