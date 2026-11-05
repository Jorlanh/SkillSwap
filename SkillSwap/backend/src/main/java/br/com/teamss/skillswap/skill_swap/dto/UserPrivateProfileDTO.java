package br.com.teamss.skillswap.skill_swap.dto;

import java.util.Set;
import java.util.UUID;
import java.util.List;
import java.util.HashSet;
import java.time.Instant;

/**
 * DTO para o perfil PRIVADO de um usuário, visto apenas por ele mesmo. CONTÉM userId e informações sensíveis.
 */
public record UserPrivateProfileDTO(
    UUID userId,
    String username,
    String email, // Incluído info sensível
    String name,
    Instant createdAt,
    boolean verifiedBadge,
    ProfileDTO profile,
    Set<SkillDTO> skills,
    Set<String> roles,
    List<UUID> followingIds,
    // --- START: Accessibility Settings ---
    boolean librasAvatarEnabled,
    String preferredTheme,
    String accessibilitySymbol
    // --- END: Accessibility Settings ---
) {
     // Construtor canônico gerado pelo record.
     // Se precisar de lógica adicional ou inicialização, pode-se adicionar um construtor compacto ou explícito.
     // Exemplo de inicialização (embora os defaults na entidade devam cuidar disso):
     public UserPrivateProfileDTO {
         skills = (skills != null) ? skills : new HashSet<>();
         roles = (roles != null) ? roles : new HashSet<>();
         followingIds = (followingIds != null) ? List.copyOf(followingIds) : List.of();
     }
}

