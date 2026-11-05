package br.com.teamss.skillswap.skill_swap.model.config;

import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Autoriza as operações administrativas usando o papel persistido no banco.
 *
 * O token do Firebase pode não carregar a claim "permissions" depois que um
 * usuário é promovido pelo painel. Nesse caso, consultar o papel persistido
 * evita que o painel mostre um administrador, mas a API responda 403.
 */
@Component("platformAdminAuthorization")
public class PlatformAdminAuthorization {

    private final UserRepository userRepository;

    public PlatformAdminAuthorization(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public boolean isAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        boolean tokenAdmin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                || "ADMIN".equals(authority.getAuthority()));
        if (tokenAdmin) {
            return true;
        }

        String email = null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            email = jwt.getClaimAsString("email");
        }
        if (email == null || email.isBlank()) {
            email = authentication.getName();
        }
        if (email == null || email.isBlank()) {
            return false;
        }

        final String lookupEmail = email;
        return userRepository.findByEmail(lookupEmail)
            .map(User::getRoles)
            .map(roles -> roles.stream().anyMatch(role -> {
                String normalized = role == null || role.getName() == null ? "" : role.getName().toUpperCase();
                return "ADMIN".equals(normalized) || "ROLE_ADMIN".equals(normalized);
            }))
            .orElse(false);
    }
}
