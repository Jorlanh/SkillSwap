package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.sql.Date;

/**
 * Campos permitidos na atualização da própria conta.
 * A API não recebe mais a entidade JPA User diretamente.
 */
public record UserUpdateRequest(
    @Pattern(regexp = "^.{0,50}$", message = "Usuário muito longo") String username,
    @Pattern(regexp = "^.{0,120}$", message = "Nome muito longo") String name,
    Date birthDate,
    @Pattern(regexp = "^.{0,40}$", message = "Telefone muito longo") String phoneNumber,
    @Pattern(regexp = "^.{0,500}$", message = "Biografia muito longa") String bio,
    @Pattern(regexp = "^.{0,100}$", message = "País muito longo") String country,
    @Pattern(regexp = "^.{0,100}$", message = "Cidade muito longa") String city,
    @Pattern(regexp = "^.{0,100}$", message = "Estado muito longo") String state,
    @Valid ProfileUpdateRequest profile,
    boolean librasAvatarEnabled,
    @Pattern(regexp = "^(default|high-contrast|high-contrast-dark|high-contrast-light)?$", message = "Tema inválido") String preferredTheme
) {
    public record ProfileUpdateRequest(
        @Pattern(regexp = "^.{0,5000}$", message = "Descrição muito longa") String description,
        @Pattern(regexp = "^.{0,255}$", message = "Localização muito longa") String location,
        @Pattern(regexp = "^.{0,500}$", message = "Informação de contato muito longa") String contactInfo,
        @Pattern(regexp = "^.{0,2000}$", message = "Links de redes sociais muito longos") String socialMediaLinks,
        @Pattern(regexp = "^.{0,80}$", message = "Status de disponibilidade muito longo") String availabilityStatus,
        @Pattern(regexp = "^.{0,1000}$", message = "Interesses muito longos") String interests,
        @Pattern(regexp = "^.{0,80}$", message = "Nível de experiência muito longo") String experienceLevel,
        @Pattern(regexp = "^.{0,120}$", message = "Formação muito longa") String educationLevel,
        Boolean isPrivate
    ) {}
}
