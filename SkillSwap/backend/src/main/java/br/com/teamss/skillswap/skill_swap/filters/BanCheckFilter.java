package br.com.teamss.skillswap.skill_swap.filters;

import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.services.LocalUserService;
import br.com.teamss.skillswap.skill_swap.model.entities.Ban;
import br.com.teamss.skillswap.skill_swap.model.repositories.BanRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
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

    public BanCheckFilter(LocalUserService localUserService, BanRepository banRepository) {
        this.localUserService = localUserService;
        this.banRepository = banRepository;
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

    private boolean hasScopedBan(java.util.UUID userId, HttpServletRequest request) {
        String path = request.getRequestURI().toLowerCase();
        String method = request.getMethod();
        if (!("POST".equals(method) || "PUT".equals(method) || "PATCH".equals(method) || "DELETE".equals(method))) return false;
        return banRepository.findByUser_UserIdAndActiveTrue(userId).stream()
            .filter(ban -> ban.getExpiresAt() == null || ban.getExpiresAt().isAfter(java.time.Instant.now()))
            .anyMatch(ban -> ("POSTS".equalsIgnoreCase(ban.getScope()) && path.contains("/posts"))
                || ("COMMENTS".equalsIgnoreCase(ban.getScope()) && path.contains("comment")));
    }
}
