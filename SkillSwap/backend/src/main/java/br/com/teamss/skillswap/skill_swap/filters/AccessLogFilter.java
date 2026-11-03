package br.com.teamss.skillswap.skill_swap.filters;

import br.com.teamss.skillswap.skill_swap.model.services.AccessLogService;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;

@Component
public class AccessLogFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(AccessLogFilter.class);

    @Autowired
    private AccessLogService accessLogService;

    @Autowired
    private UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Passa o filtro para frente primeiro
        filterChain.doFilter(request, response);
        
        // Agora, verifica e registra o acesso após a requisição ser processada
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication.getPrincipal() instanceof String)) {
            String requestPath = request.getRequestURI();
            if (!requestPath.endsWith("/api/access-logs/me") && !"OPTIONS".equalsIgnoreCase(request.getMethod())) {
                String email = null;
                if (authentication.getPrincipal() instanceof Jwt jwt) {
                    email = jwt.getClaimAsString("email");
                }
                if (email != null && !email.isBlank()) {
                    try {
                        userRepository.findByEmail(email).map(User::getUserId)
                            .ifPresent(userId -> accessLogService.logAccess(userId, request));
                    } catch (RuntimeException ex) {
                        // O histórico é complementar e não pode derrubar a resposta principal.
                        logger.debug("Não foi possível registrar o acesso do usuário autenticado", ex);
                    }
                }
            }
        }
    }
}
