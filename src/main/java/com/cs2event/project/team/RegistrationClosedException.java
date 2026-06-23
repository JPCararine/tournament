package com.cs2event.project.team;

/**
 * Lançada quando o campeonato atingiu o limite de vagas (pendentes +
 * confirmadas) e as inscrições estão encerradas. Resulta em HTTP 409.
 */
public class RegistrationClosedException extends RuntimeException {

    public RegistrationClosedException(String message) {
        super(message);
    }
}
