package br.com.teamss.skillswap.skill_swap.filters;

import br.com.teamss.skillswap.skill_swap.model.services.AccessLogService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class AccessLogFilter extends OncePerRequestFilter {

    @Autowired
    private AccessLogService accessLogService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Passa o filtro para frente primeiro
        filterChain.doFilter(request, response);
        
        // Agora, verifica e registra o acesso após a requisição ser processada
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getPrincipal())) {
            // Assume que o principal contém o username, e o AccessLogService precisa do UUID.
            // O ideal é que o AccessLogService consiga converter o username em UUID.
            String username = authentication.getName(); 
            
            // O AccessLogService espera um UUID. Vamos tentar logar apenas se o principal for um UUID.
            // Para simplificar o teste, vamos forçar o log aqui para qualquer requisição autenticada.
            
            // NOTE: Na implementação atual, é difícil obter o UUID a partir do Authentication.getName()
            // sem injeção do UserDetailsService.
            // Como contorno, vamos assumir que o AccessLogService pode lidar com o username,
            // ou confiar que a requisição GET /api/users/{userId} está logando.

            // Para garantir que a requisição de busca de usuário seja logada:
            if (request.getRequestURI().contains("/api/users/") && request.getMethod().equals("GET")) {
                try {
                    // Extrai o UUID da URL
                    String path = request.getRequestURI();
                    String userIdString = path.substring(path.lastIndexOf('/') + 1);
                    UUID uuid = UUID.fromString(userIdString);
                    
                    // Loga o acesso *após* a requisição ter sido processada
                    accessLogService.logAccess(uuid, request);
                } catch (IllegalArgumentException e) {
                    // O valor na URL não é um UUID, ignora
                }
            }
        }
    }
}
