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
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Agregado raiz da inscrição de uma equipe no campeonato.
 *
 * <p>O status nasce como {@link TeamStatus#PENDENTE} e só pode ser promovido a
 * {@link TeamStatus#CONFIRMADA} pelo webhook de pagamento — nunca pelo front.</p>
 */
@Entity
@Table(name = "team")
@Getter
@Setter
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private String teamName;

    @Column(nullable = false)
    private String captainName;

    @Column(nullable = false)
    private String captainEmail;

    @Column(nullable = false)
    private String captainDiscordId;

    @Column(nullable = false)
    private String whatsapp;

    /**
     * Disponibilidade (ex.: "Segunda - Manhã"). Mapeada como @ElementCollection
     * para portabilidade entre Postgres e bancos de teste. Alternativa: coluna jsonb
     * via @JdbcTypeCode(SqlTypes.JSON) — ver README.
     */
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

    /** Id da cobrança no AbacatePay — chave para casar com o webhook. */
    @Column(unique = true)
    private String billingId;

    /** Valor da taxa de inscrição em centavos. */
    private Integer amountCents;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant confirmedAt;
}
