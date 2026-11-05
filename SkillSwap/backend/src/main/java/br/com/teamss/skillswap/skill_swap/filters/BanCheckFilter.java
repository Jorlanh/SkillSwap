package br.com.teamss.skillswap.skill_swap.filters;

import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.services.LocalUserService;
import br.com.teamss.skillswap.skill_swap.model.entities.Ban;
import br.com.teamss.skillswap.skill_swap.model.repositories.BanRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.AccessLogRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class BanCheckFilter extends OncePerRequestFilter {

    private final LocalUserService localUserService;
    private final BanRepository banRepository;
    private final AccessLogRepository accessLogRepository;

    public BanCheckFilter(LocalUserService localUserService, BanRepository banRepository, AccessLogRepository accessLogRepository) {
        this.localUserService = localUserService;
        this.banRepository = banRepository;
        this.accessLogRepository = accessLogRepository;
    }

    @Override
    protected void doFilterInternal(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof Jwt jwt) {
            try {
                if (hasActiveIpBan(request)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado para este endereço de rede.");
                    return;
                }
                String tokenHash = hashBearerToken(request.getHeader("Authorization"));
                if (tokenHash != null && accessLogRepository.existsByTokenHashAndRevokedTrue(tokenHash)) {
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Este dispositivo foi desconectado.");
                    return;
                }
                User user = localUserService.findOrCreateUser(jwt);
                if (user.isBanned() || hasScopedBan(user.getUserId(), request)) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado.");
                    return;
                }

                // Firebase authenticates identity; the local database owns operational roles.
                var authorities = new ArrayList<GrantedAuthority>(authentication.getAuthorities());
                user.getRoles().forEach(role -> {
                    String roleName = role.getName();
                    String authority = roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName;
                    if (authorities.stream().noneMatch(item -> authority.equals(item.getAuthority()))) {
                        authorities.add(new SimpleGrantedAuthority(authority));
                    }
                });
                SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(jwt, authentication.getCredentials(), authorities));
            } catch (IllegalArgumentException exception) {
                logger.warn("Token sem identidade local válida: " + exception.getMessage());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token de usuário inválido.");
                return;
            }
        }
        filterChain.doFilter(request, response);
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

    private boolean hasScopedBan(java.util.UUID userId, HttpServletRequest request) {
        String path = request.getRequestURI().toLowerCase();
        String method = request.getMethod();
        if (!("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method))) return false;
        return banRepository.findByUser_UserIdAndActiveTrue(userId).stream()
            .filter(ban -> ban.getExpiresAt() == null || ban.getExpiresAt().isAfter(java.time.Instant.now()))
            .anyMatch(ban -> ("POSTS".equalsIgnoreCase(ban.getScope()) && path.contains("/posts"))
                || ("COMMENTS".equalsIgnoreCase(ban.getScope()) && path.contains("comment")));
    }

    private boolean hasActiveIpBan(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        String ip = forwarded == null || forwarded.isBlank() ? request.getRemoteAddr() : forwarded.split(",")[0].trim();
        if (ip == null || ip.isBlank()) return false;
        return banRepository.findByIpAddressAndActiveTrue(ip).stream()
            .anyMatch(ban -> ban.getExpiresAt() == null || ban.getExpiresAt().isAfter(java.time.Instant.now()));
    }
}
