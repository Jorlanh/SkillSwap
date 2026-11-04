package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.AccessLogDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.AccessLog;
import br.com.teamss.skillswap.skill_swap.model.repositories.AccessLogRepository;
import br.com.teamss.skillswap.skill_swap.model.services.AccessLogService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.time.Instant;

@Service
public class AccessLogServiceImpl implements AccessLogService {

    @Autowired
    private AccessLogRepository accessLogRepository;

    @Override
    public void logAccess(UUID userId, HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ipAddress = forwarded == null || forwarded.isBlank()
            ? request.getRemoteAddr()
            : forwarded.split(",")[0].trim();

        // Cria e salva o registro de acesso sem informações de localização.
        AccessLog accessLog = new AccessLog(
            userId,
            ipAddress,
            "Local não informado", // O servidor não faz geolocalização automática.
            null,
            null,
            null
        );
        accessLog.setAccessTime(Instant.now());
        accessLogRepository.save(accessLog);
    }

    @Override
    public List<AccessLogDTO> getAccessHistory(UUID userId) {
        List<AccessLog> accessLogs = accessLogRepository.findByUserIdOrderByAccessTimeDesc(userId);
        return accessLogs.stream()
                .map(log -> new AccessLogDTO(
                    log.getAccessTime(),
                    log.getIpAddress(),
                    log.getLocation(),
                    log.getCity(),
                    log.getSubdivision(),
                    log.getCountry()
                ))
                .collect(Collectors.toList());
    }
}
