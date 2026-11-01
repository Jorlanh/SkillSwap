package br.com.teamss.skillswap.skill_swap.dto;

import java.util.Set;

// DTO para o perfil PÚBLICO de um usuário, visto por outros. NÃO CONTÉM userId.
public record UserPublicProfileDTO(
    String username,
    String name,
    boolean verifiedBadge, // NOVO CAMPO
    ProfileDTO profile,
    Set<SkillDTO> skills
) {}