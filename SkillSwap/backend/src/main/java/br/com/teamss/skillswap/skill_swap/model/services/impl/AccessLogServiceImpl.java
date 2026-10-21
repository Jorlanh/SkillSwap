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

@Service
public class AccessLogServiceImpl implements AccessLogService {

    @Autowired
    private AccessLogRepository accessLogRepository;

    @Override
    public void logAccess(UUID userId, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();

        // Cria e salva o registro de acesso sem informações de localização.
        AccessLog accessLog = new AccessLog(
            userId,
            ipAddress,
            "N/A", // location
            "N/A", // city
            "N/A", // subdivision
            "N/A"  // country
        );
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