package br.com.teamss.skillswap.skill_swap.model.exception;

import br.com.teamss.skillswap.skill_swap.model.services.SecurityAuditService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @Autowired
    private SecurityAuditService auditService;

    // --- NOVO: Tratamento de Nomes de Usuário Duplicados ---
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<?> handleUniqueConstraintViolation(DataIntegrityViolationException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(Map.of("message", "O nome de usuário ou e-mail já está em uso. Escolha outro."));
    }

    @ExceptionHandler(InappropriateContentException.class)
    public ProblemDetail handleInappropriateContent(InappropriateContentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
        problem.setTitle("Conteúdo Inapropriado");
        problem.setType(URI.create("urn:problem-type:inappropriate-content"));
        return problem;
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ProblemDetail handleNoResourceFound(NoResourceFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "O recurso solicitado não foi encontrado: " + ex.getResourcePath());
        problem.setTitle("Recurso Não Encontrado");
        problem.setType(URI.create("urn:problem-type:not-found"));
        return problem;
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ProblemDetail handleEntityNotFound(EntityNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Recurso Não Encontrado");
        problem.setType(URI.create("urn:problem-type:not-found"));
        return problem;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ProblemDetail handleIllegalState(IllegalStateException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Conflito de Estado da Requisição");
        problem.setType(URI.create("urn:problem-type:conflict"));
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail handleIllegalArgument(IllegalArgumentException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Requisição Inválida");
        problem.setType(URI.create("urn:problem-type:bad-request"));
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        String username = (SecurityContextHolder.getContext().getAuthentication() != null) ?
                          SecurityContextHolder.getContext().getAuthentication().getName() : "ANONYMOUS";
        
        auditService.logAccessDenied(username, request.getRequestURI(), request);
        
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Acesso Negado");
        problem.setType(URI.create("urn:problem-type:forbidden"));
        return problem;
    }
}