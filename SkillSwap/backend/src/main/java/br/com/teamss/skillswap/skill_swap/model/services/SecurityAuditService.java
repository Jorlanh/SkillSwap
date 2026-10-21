package br.com.teamss.skillswap.skill_swap.model.services;

import jakarta.servlet.http.HttpServletRequest;

public interface SecurityAuditService {
    void logLoginSuccess(String username, HttpServletRequest request);
    void logLoginFailure(String username, String reason, HttpServletRequest request);
    void logAccessDenied(String username, String resource, HttpServletRequest request);
    void logPasswordChangeSuccess(String username, HttpServletRequest request);
    void logPasswordChangeFailure(String username, String reason, HttpServletRequest request);
}