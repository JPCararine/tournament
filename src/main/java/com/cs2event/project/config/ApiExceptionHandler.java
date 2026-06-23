package com.cs2event.project.config;

import com.cs2event.project.team.DuplicateRegistrationException;
import com.cs2event.project.team.RegistrationClosedException;
import com.cs2event.project.payment.AsaasClient;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.put(fe.getField(), fe.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Dados de inscrição inválidos");
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(DuplicateRegistrationException.class)
    public ResponseEntity<Map<String, Object>> handleDuplicate(DuplicateRegistrationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Ja existe uma inscricao com estes dados.");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(RegistrationClosedException.class)
    public ResponseEntity<Map<String, Object>> handleRegistrationClosed(RegistrationClosedException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        body.put("registrationOpen", false);
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }

    @ExceptionHandler(AsaasClient.AsaasException.class)
    public ResponseEntity<Map<String, Object>> handleAsaas(AsaasClient.AsaasException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Nao foi possivel gerar o Pix no momento. Tente novamente em alguns minutos.");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }
}
