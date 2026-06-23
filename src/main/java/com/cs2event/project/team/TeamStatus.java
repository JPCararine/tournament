package com.cs2event.project.team;

/**
 * Situação da inscrição de uma equipe.
 *
 * <p>PENDENTE: manifestou interesse (formulário enviado), aguardando pagamento.
 * CONFIRMADA: pagamento da taxa confirmado via webhook do AbacatePay.</p>
 */
public enum TeamStatus {
    PENDENTE,
    CONFIRMADA
}
