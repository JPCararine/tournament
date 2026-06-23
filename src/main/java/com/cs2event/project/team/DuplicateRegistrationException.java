package com.cs2event.project.team;

/**
 * Lançada quando uma inscrição reutiliza um dado que já pertence a outra
 * equipe (nome da equipe, e-mail, Discord ou WhatsApp). Resulta em HTTP 409.
 */
public class DuplicateRegistrationException extends RuntimeException {

    public DuplicateRegistrationException(String message) {
        super(message);
    }
}
