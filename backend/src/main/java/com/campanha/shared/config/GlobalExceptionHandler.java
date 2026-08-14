package com.campanha.shared.config;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentials(BadCredentialsException e) {
        return build(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(AccessDeniedException e) {
        // Mensagem em UPPER_SNAKE_CASE vira código explícito (ex: MODO_CAMPO_INATIVO).
        String msg = e.getMessage() == null ? "" : e.getMessage();
        String codigo = msg.matches("[A-Z_]+") ? msg : "ACESSO_NEGADO";
        return build(HttpStatus.FORBIDDEN, codigo, msg);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, "VALIDACAO", msg);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraint(ConstraintViolationException e) {
        return build(HttpStatus.BAD_REQUEST, "VALIDACAO", e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArg(IllegalArgumentException e) {
        return build(HttpStatus.BAD_REQUEST, "ARGUMENTO_INVALIDO", e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(IllegalStateException e) {
        return build(HttpStatus.CONFLICT, "ESTADO_INVALIDO", e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception e) {
        log.error("Erro não tratado", e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ERRO_INTERNO", "Ocorreu um erro inesperado.");
    }

    private ResponseEntity<Map<String, Object>> build(HttpStatus status, String codigo, String mensagem) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("codigo", codigo);
        body.put("mensagem", mensagem);
        return ResponseEntity.status(status).body(body);
    }
}
