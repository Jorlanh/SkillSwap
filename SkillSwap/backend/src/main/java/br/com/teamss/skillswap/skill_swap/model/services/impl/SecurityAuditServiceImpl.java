package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.SecurityAuditService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SecurityAuditServiceImpl implements SecurityAuditService {

    private static final Logger auditLogger = LoggerFactory.getLogger("security-audit");

    private String getIpAddress(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @Override
    public void logLoginSuccess(String username, HttpServletRequest request) {
        String ipAddress = getIpAddress(request);
        auditLogger.info("EVENT=LOGIN_SUCCESS; USERNAME={}; IP_ADDRESS={};", username, ipAddress);
    }

    @Override
    public void logLoginFailure(String username, String reason, HttpServletRequest request) {
        String ipAddress = getIpAddress(request);
        auditLogger.warn("EVENT=LOGIN_FAILURE; USERNAME={}; IP_ADDRESS={}; REASON={};", username, ipAddress, reason);
    }

    @Override
    public void logAccessDenied(String username, String resource, HttpServletRequest request) {
        String ipAddress = getIpAddress(request);
        auditLogger.warn("EVENT=ACCESS_DENIED; USERNAME={}; IP_ADDRESS={}; RESOURCE={};", username, ipAddress, resource);
    }
    
    @Override
    public void logPasswordChangeSuccess(String username, HttpServletRequest request) {
        String ipAddress = getIpAddress(request);
        auditLogger.info("EVENT=PASSWORD_CHANGE_SUCCESS; USERNAME={}; IP_ADDRESS={};", username, ipAddress);
    }

    @Override
    public void logPasswordChangeFailure(String username, String reason, HttpServletRequest request) {
        String ipAddress = getIpAddress(request);
        auditLogger.warn("EVENT=PASSWORD_CHANGE_FAILURE; USERNAME={}; IP_ADDRESS={}; REASON={};", username, ipAddress, reason);
    }
}