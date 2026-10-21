package br.com.teamss.skillswap.skill_swap.filters;

import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.services.LocalUserService;
import br.com.teamss.skillswap.skill_swap.model.entities.Ban;
import br.com.teamss.skillswap.skill_swap.model.repositories.BanRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.time.Instant;
import java.util.List;
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
    private final UserRepository userRepository;

    public BanCheckFilter(LocalUserService localUserService, BanRepository banRepository, UserRepository userRepository) {
        this.localUserService = localUserService;
        this.banRepository = banRepository;
        this.userRepository = userRepository;
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
                List<Ban> activeBans = banRepository.findByUser_UserIdAndActiveTrue(user.getUserId());
                Instant now = Instant.now();
                boolean hasActiveBan = activeBans.stream().anyMatch(ban ->
                        ban.getExpiresAt() == null || ban.getExpiresAt().isAfter(now));
                boolean legacyOrPermanentBan = user.isBanned() && activeBans.isEmpty();
                if (hasActiveBan || legacyOrPermanentBan) {
                    response.sendError(HttpServletResponse.SC_FORBIDDEN, "Acesso negado.");
                    return;
                }
                // Expiração de suspensão temporária deve liberar a conta sem
                // depender de ação manual do administrador.
                if (user.isBanned()) {
                    user.setBanned(false);
                    userRepository.save(user);
                }
                activeBans.stream()
                        .filter(ban -> ban.getExpiresAt() != null && !ban.getExpiresAt().isAfter(now))
                        .forEach(ban -> ban.setActive(false));
                if (!activeBans.isEmpty()) {
                    banRepository.saveAll(activeBans);
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
}
