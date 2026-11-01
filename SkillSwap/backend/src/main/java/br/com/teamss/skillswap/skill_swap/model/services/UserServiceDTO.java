package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserPrivateProfileDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserPublicProfileDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserSummaryDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.User;

import java.util.List;
import java.util.UUID;

public interface UserServiceDTO {
    UserDTO toUserDTO(User user); // Assinatura original
    List<UserSummaryDTO> findAllSummaries(); // Assinatura original
    UserDTO findByIdDTO(UUID id); // Assinatura original
    UserSummaryDTO findSummaryByIdDTO(UUID id); // Assinatura original
    void updateVerificationCode(UUID userId, String code); // Assinatura original
    void updateVerificationStatus(UUID userId, boolean verified); // Assinatura original

    /** @deprecated Use métodos de atualização mais específicos. */
    @Deprecated
    void saveUserDTO(UserDTO userDTO); // Assinatura original (marcada como deprecated)

    UserDTO findByUsernameDTO(String username); // Assinatura original
    UserDTO getAuthenticatedUser(); // Assinatura original

    // NOVOS MÉTODOS ADICIONADOS originalmente
    UserPublicProfileDTO toUserPublicProfileDTO(User user); // Assinatura original
    UserPrivateProfileDTO toUserPrivateProfileDTO(User user); // Assinatura original
    UserPublicProfileDTO findPublicProfileByUsername(String username); // Assinatura original

    // --- START: Accessibility Settings Specific Update DTO Method ---
    /**
     * Atualiza as configurações de acessibilidade para o usuário especificado e retorna o DTO atualizado.
     * @param userId O ID do usuário.
     * @param librasAvatarEnabled O novo valor para habilitar o avatar LIBRAS.
     * @param preferredTheme A nova string do tema preferido.
     * @return O UserDTO atualizado refletindo as alterações.
     */
    UserDTO updateAccessibilitySettingsDTO(UUID userId, boolean librasAvatarEnabled, String preferredTheme);
    // --- END: Accessibility Settings Specific Update DTO Method ---
}

