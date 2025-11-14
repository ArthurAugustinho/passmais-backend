package com.passmais.interfaces.handler;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.BindException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.core.NestedExceptionUtils;

import com.passmais.domain.exception.ApiErrorException;
import com.passmais.application.service.exception.InviteSecurityException;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("mensagem", "Erro de validação");
        Map<String, String> erros = new HashMap<>();
        for (var error : ex.getBindingResult().getAllErrors()) {
            String field = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            erros.put(field, error.getDefaultMessage());
        }
        body.put("erros", erros);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("mensagem", "Erro de validação");
        Map<String, String> erros = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(v -> v.getPropertyPath().toString(), v -> v.getMessage()));
        body.put("erros", erros);
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = "Violação de integridade de dados";

        String constraintName = null;
        Throwable cause = ex.getCause();
        if (cause instanceof org.hibernate.exception.ConstraintViolationException hce) {
            constraintName = hce.getConstraintName();
        }

        if (constraintName != null) {
            message = translateConstraint(constraintName);
        } else {
            String details = NestedExceptionUtils.getMostSpecificCause(ex).getMessage();
            message = translateFromDetails(details);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("mensagem", message);
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    private String translateConstraint(String constraintName) {
        if (constraintName == null) return "Violação de integridade de dados";
        return switch (constraintName) {
            case "users_email_key" -> "E-mail já cadastrado";
            case "uk_doctor_profiles_phone", "uk_patient_profiles_cell_phone" -> "Telefone já utilizado";
            case "uk_users_cpf" -> "CPF já utilizado";
            default -> "Registro já existe";
        };
    }

    private String translateFromDetails(String details) {
        if (details == null) return "Violação de integridade de dados";
        String lower = details.toLowerCase();
        if (lower.contains("users_email_key") || lower.contains("(email)")) {
            return "E-mail já cadastrado";
        }
        if (lower.contains("uk_doctor_profiles_phone") || lower.contains("uk_patient_profiles_cell_phone") || lower.contains("(phone)") || lower.contains("(cell_phone)")) {
            return "Telefone já utilizado";
        }
        if (lower.contains("cpf")) {
            return "CPF já utilizado";
        }
        if (lower.contains("crm")) {
            return "CRM já utilizado";
        }
        if (lower.contains("23505") || lower.contains("duplicate key") || lower.contains("unique")) {
            return "Registro já existe";
        }
        return "Violação de integridade de dados";
    }

    @Override
    protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("mensagem", "Erro de validação");
        Map<String, String> erros = new HashMap<>();
        for (var error : ex.getBindingResult().getAllErrors()) {
            String field = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
            erros.put(field, error.getDefaultMessage());
        }
        body.put("erros", erros);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("mensagem", "Corpo da requisição inválido ou malformado");
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("mensagem", "Parâmetro obrigatório ausente: " + ex.getParameterName());
        return handleExceptionInternal(ex, body, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("mensagem", "Método não suportado: " + ex.getMethod());
        return handleExceptionInternal(ex, body, headers, HttpStatus.METHOD_NOT_ALLOWED, request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Object> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("mensagem", "Tipo inválido para parâmetro: " + ex.getName());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InviteSecurityException.class)
    public ResponseEntity<Object> handleInviteSecurity(InviteSecurityException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("error", ex.getMessage());
        body.put("status", ex.getStatus().value());
        return new ResponseEntity<>(body, ex.getStatus());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Object> handleIllegalArgument(IllegalArgumentException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("mensagem", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Object> handleBadCredentials(BadCredentialsException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("mensagem", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGeneric(Exception ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("mensagem", "Erro interno inesperado");
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ApiErrorException.class)
    public ResponseEntity<Object> handleApiError(ApiErrorException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("code", ex.getCode());
        body.put("message", ex.getMessage());
        if (ex.getFieldErrors() != null && !ex.getFieldErrors().isEmpty()) {
            List<Map<String, String>> errors = ex.getFieldErrors().entrySet().stream()
                    .map(entry -> Map.of("field", entry.getKey(), "message", entry.getValue()))
                    .toList();
            body.put("errors", errors);
        }
        return new ResponseEntity<>(body, ex.getStatus());
    }
}
