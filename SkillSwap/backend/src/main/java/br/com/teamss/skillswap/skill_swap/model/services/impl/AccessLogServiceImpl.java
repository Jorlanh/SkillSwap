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
import java.time.Duration;
import java.util.Locale;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

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

        String userAgent = request.getHeader("User-Agent");
        String deviceInfo = describeDevice(userAgent);
        String tokenHash = hashBearerToken(request.getHeader("Authorization"));
        Instant now = Instant.now();
        var recent = accessLogRepository.findByUserIdOrderByAccessTimeDesc(userId).stream()
            .filter(log -> !log.isRevoked())
            .filter(log -> tokenHash != null && tokenHash.equals(log.getTokenHash()))
            .filter(log -> log.getAccessTime() != null && Duration.between(log.getAccessTime(), now).toMinutes() < 30)
            .findFirst();
        if (recent.isPresent()) return;

        AccessLog accessLog = new AccessLog(
            userId,
            ipAddress,
            "Local não informado", // O servidor não faz geolocalização automática.
            null,
            null,
            null
        );
        accessLog.setDeviceInfo(deviceInfo);
        accessLog.setTokenHash(tokenHash);
        accessLog.setAccessTime(now);
        accessLogRepository.save(accessLog);
    }

    @Override
    public List<AccessLogDTO> getAccessHistory(UUID userId) {
        List<AccessLog> accessLogs = accessLogRepository.findByUserIdOrderByAccessTimeDesc(userId);
        // Registros antigos da primeira versão não tinham data nem dispositivo.
        // Eles não devem aparecer como se fossem acessos reais com "N/A".
        return accessLogs.stream().filter(log -> !log.isRevoked() && log.getAccessTime() != null)
                .map(log -> new AccessLogDTO(
                    log.getAccessId(),
                    log.getAccessTime(),
                    log.getIpAddress(),
                    log.getLocation(),
                    log.getCity(),
                    log.getSubdivision(),
                    log.getCountry(),
                    log.getDeviceInfo()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void updateLatestLocation(UUID userId, double latitude, double longitude) {
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Coordenadas inválidas.");
        }
        accessLogRepository.findByUserIdOrderByAccessTimeDesc(userId).stream()
            .filter(log -> !log.isRevoked() && log.getAccessTime() != null)
            .findFirst().ifPresent(log -> {
            log.setLocation(String.format(Locale.US, "%.6f, %.6f", latitude, longitude));
            log.setCity("Localização do dispositivo");
            accessLogRepository.save(log);
        });
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void revokeAccess(UUID userId, Long accessId) {
        accessLogRepository.findById(accessId).ifPresent(log -> {
            if (!userId.equals(log.getUserId())) {
                throw new org.springframework.security.access.AccessDeniedException("Acesso não pertence ao usuário.");
            }
            log.setRevoked(true);
            accessLogRepository.save(log);
        });
    }

    private String hashBearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return null;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(authorization.substring(7).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (Exception exception) {
            return null;
        }
    }

    private String describeDevice(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Navegador não identificado";
        String browser = userAgent.contains("Edg/") ? "Microsoft Edge" : userAgent.contains("Chrome/") ? "Google Chrome" : userAgent.contains("Firefox/") ? "Mozilla Firefox" : userAgent.contains("Safari/") ? "Safari" : "Navegador";
        String platform = userAgent.contains("Android") ? "Android" : userAgent.contains("iPhone") || userAgent.contains("iPad") ? "iOS" : userAgent.contains("Windows") ? "Windows" : userAgent.contains("Mac OS") ? "macOS" : userAgent.contains("Linux") ? "Linux" : "dispositivo desconhecido";
        return browser + " · " + platform;
    }
}
