package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.AccessLogDTO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.UUID;

public interface AccessLogService {
    void logAccess(UUID userId, HttpServletRequest request); // Registra um acesso
    List<AccessLogDTO> getAccessHistory(UUID userId); // Retorna o histórico de acessos
}