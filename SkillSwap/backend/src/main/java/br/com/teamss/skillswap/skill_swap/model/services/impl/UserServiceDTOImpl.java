package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.*;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.Role;
import br.com.teamss.skillswap.skill_swap.model.entities.Skill; 
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.AccessibilityPreferencesRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.UserService; 
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import br.com.teamss.skillswap.skill_swap.model.services.LocalUserService;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication; 
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceDTOImpl implements UserServiceDTO {

    private final UserRepository userRepository;
    private final UserService userService;
    private final LocalUserService localUserService;
    private final AccessibilityPreferencesRepository accessibilityPreferencesRepository;

    @Autowired 
    public UserServiceDTOImpl(
        UserRepository userRepository,
        UserService userService,
        LocalUserService localUserService,
        AccessibilityPreferencesRepository accessibilityPreferencesRepository
    ) {
        this.userRepository = userRepository;
        this.userService = userService; 
        this.localUserService = localUserService;
        this.accessibilityPreferencesRepository = accessibilityPreferencesRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO toUserDTO(User user) {
        if (user == null) {
            return null; 
        }

        user = loadUserGraph(user);

        ProfileDTO profileDTO = null;
        if (user.getProfile() != null) {
            Profile profile = user.getProfile();
            profileDTO = new ProfileDTO(
                    profile.getProfileId(),
                    profile.getDescription(),
                    profile.getImageUrl(),
                    profile.getLocation(),
                    profile.getContactInfo(),
                    profile.getSocialMediaLinks(),
                    profile.getAvailabilityStatus(),
                    profile.getInterests(),
                    profile.getExperienceLevel(),
                    profile.getEducationLevel());
        }

        Set<String> roles = (user.getRoles() != null) ? user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet()) : Collections.emptySet();

        Set<SkillDTO> skills = (user.getSkills() != null) ? user.getSkills().stream()
                .map(skill -> new SkillDTO(
                        skill.getSkillId(),
                        skill.getName(),
                        skill.getDescription(),
                        skill.getCategory(),
                        skill.getLevel()
                ))
                .collect(Collectors.toSet()) : Collections.emptySet(); 

        UserDTO userDTO = new UserDTO(); 
        userDTO.setUserId(user.getUserId());
        userDTO.setUsername(user.getUsername());
        userDTO.setRoles(roles);
        userDTO.setProfile(profileDTO);
        userDTO.setSkills(skills);
        userDTO.setEmail(user.getEmail());
        userDTO.setPhoneNumber(user.getPhoneNumber());
        // Nunca serializar segredos de segundo fator ou códigos de verificação.

        // --- START: Accessibility Settings ---
        userDTO.setLibrasAvatarEnabled(user.isLibrasAvatarEnabled());
        userDTO.setPreferredTheme(user.getPreferredTheme());
        // --- END: Accessibility Settings ---

        return userDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserSummaryDTO> findAllSummaries() {
        return userRepository.findAll().stream()
                .map(user -> {
                    UserSummaryDTO summary = new UserSummaryDTO(user.getUsername(), user.getName(), user.isVerifiedBadge());
                    summary.setUserId(user.getUserId());
                    summary.setAvatarUrl(user.getProfile() == null ? null : user.getProfile().getImageUrl());
                    return summary;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO findByIdDTO(UUID id) {
        User user = userService.findById(id);
        return toUserDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserSummaryDTO findSummaryByIdDTO(UUID id) {
        User user = userService.findById(id);
        UserSummaryDTO summary = new UserSummaryDTO(user.getUsername(), user.getName(), user.isVerifiedBadge());
        summary.setUserId(user.getUserId());
        summary.setAvatarUrl(user.getProfile() == null ? null : user.getProfile().getImageUrl());
        return summary;
    }

    @Override
    @Transactional
    public void updateVerificationCode(UUID userId, String code) {
        User user = userService.findById(userId);
        user.setVerificationCode(code);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateVerificationStatus(UUID userId, boolean verified) {
        User user = userService.findById(userId);
        user.setVerified(verified);
        user.setVerifiedAt(verified ? Instant.now() : null);
        if (verified) { 
            user.setVerificationCode(null);
            user.setVerificationCodeExpiry(null);
        }
        userRepository.save(user); 
    }

    @Override
    @Deprecated
    @Transactional
    public void saveUserDTO(UserDTO userDTO) {
        User user = userService.findById(userDTO.getUserId());

        if(userDTO.getUsername() != null) user.setUsername(userDTO.getUsername());
        if(userDTO.getEmail() != null) user.setEmail(userDTO.getEmail());
        if(userDTO.getPhoneNumber() != null) user.setPhoneNumber(userDTO.getPhoneNumber());
        if(userDTO.getTwoFactorSecret() != null) user.setTwoFactorSecret(userDTO.getTwoFactorSecret());
        if(userDTO.getVerificationCode() != null) user.setVerificationCode(userDTO.getVerificationCode());

        user.setLibrasAvatarEnabled(userDTO.isLibrasAvatarEnabled()); 
        if(userDTO.getPreferredTheme() != null) user.setPreferredTheme(userDTO.getPreferredTheme());

        userRepository.save(user); 
    }


    @Override
    @Transactional(readOnly = true)
    public UserDTO findByUsernameDTO(String username) {
        User user = userRepository.findByUsername(username) 
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com username: " + username));
        return toUserDTO(user);
    }

    @Override
    @Transactional
    public UserDTO getAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal() instanceof String && "anonymousUser".equals(authentication.getPrincipal())) {
            throw new IllegalStateException("Nenhum usuário autenticado encontrado no contexto de segurança.");
        }

        Object principal = authentication.getPrincipal();
        User user;

        // SE O TOKEN FOR DO AUTH0 (JWT)
        if (principal instanceof Jwt) {
            Jwt jwt = (Jwt) principal;
            // O primeiro acesso cria e registra o usuário local e seu perfil.
            // O Firebase continua sendo a fonte da autenticação; o PostgreSQL
            // guarda apenas os dados operacionais necessários à plataforma.
            user = localUserService.findOrCreateUser(jwt);
        } 
        // SE FOR O SISTEMA ANTIGO DE SESSÃO LOCAL (UserDetails)
        else if (principal instanceof UserDetails) {
            String username = ((UserDetails) principal).getUsername();
            user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado ('" + username + "') não encontrado na base de dados."));
        } 
        // CASO GENÉRICO
        else {
            String username = principal.toString();
            user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado ('" + username + "') não encontrado na base de dados."));
        }

        return toUserDTO(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicProfileDTO toUserPublicProfileDTO(User user) {
        if (user == null) return null;
        user = loadUserGraph(user);
        Profile profile = user.getProfile();
        ProfileDTO profileDTO = null;
        if (profile != null) {
            profileDTO = new ProfileDTO(
                profile.getProfileId(), profile.getDescription(), profile.getImageUrl(),
                profile.getLocation(), profile.getContactInfo(), profile.getSocialMediaLinks(),
                profile.getAvailabilityStatus(), profile.getInterests(),
                profile.getExperienceLevel(), profile.getEducationLevel()
            );
        }

        Set<SkillDTO> skills = (user.getSkills() != null) ? user.getSkills().stream()
            .map(skill -> new SkillDTO(skill.getSkillId(), skill.getName(), skill.getDescription(), skill.getCategory(), skill.getLevel()))
            .collect(Collectors.toSet()) : Collections.emptySet();

        String accessibilitySymbol = accessibilityPreferencesRepository.findById(user.getUserId())
            .map(preferences -> preferences.getAccessibilitySymbol())
            .orElse(null);

        return new UserPublicProfileDTO(
            user.getUserId(), user.getUsername(), user.getName(), user.getCreatedAt(), user.isVerifiedBadge(), profileDTO, skills,
            accessibilitySymbol
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserPrivateProfileDTO toUserPrivateProfileDTO(User user) {
        if (user == null) return null;
        user = loadUserGraph(user);
        Profile profile = user.getProfile();
        ProfileDTO profileDTO = null;
        if (profile != null) {
            profileDTO = new ProfileDTO(
                profile.getProfileId(), profile.getDescription(), profile.getImageUrl(),
                profile.getLocation(), profile.getContactInfo(), profile.getSocialMediaLinks(),
                profile.getAvailabilityStatus(), profile.getInterests(),
                profile.getExperienceLevel(), profile.getEducationLevel()
            );
        }

        Set<SkillDTO> skills = (user.getSkills() != null) ? user.getSkills().stream()
            .map(skill -> new SkillDTO(skill.getSkillId(), skill.getName(), skill.getDescription(), skill.getCategory(), skill.getLevel()))
            .collect(Collectors.toSet()) : Collections.emptySet();

        Set<String> roles = (user.getRoles() != null) ? user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()) : Collections.emptySet();

        String accessibilitySymbol = accessibilityPreferencesRepository.findById(user.getUserId())
            .map(preferences -> preferences.getAccessibilitySymbol())
            .orElse(null);

        return new UserPrivateProfileDTO(
            user.getUserId(), user.getUsername(), user.getEmail(), user.getName(),
            user.getCreatedAt(),
            user.isVerifiedBadge(), profileDTO, skills, roles,
            // --- START: Accessibility Settings ---
            user.isLibrasAvatarEnabled(),
            user.getPreferredTheme(),
            accessibilitySymbol
            // --- END: Accessibility Settings ---
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserPublicProfileDTO findPublicProfileByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com username: " + username));
        return toUserPublicProfileDTO(user);
    }

    @Override
    @Transactional 
    public UserDTO updateAccessibilitySettingsDTO(UUID userId, boolean librasAvatarEnabled, String preferredTheme) {
        User updatedUser = userService.updateAccessibilitySettings(userId, librasAvatarEnabled, preferredTheme);
        return toUserDTO(updatedUser);
    }

    /**
     * O usuário recebido por um controller pode estar detached. Recarregamos
     * os relacionamentos usados pelos DTOs em uma única consulta para evitar
     * LazyInitializationException durante o login e a abertura do perfil.
     */
    private User loadUserGraph(User user) {
        User loaded = userRepository.findWithProfileSkillsAndRolesByUserId(user.getUserId()).orElse(user);
        Hibernate.initialize(loaded.getProfile());
        Hibernate.initialize(loaded.getSkills());
        Hibernate.initialize(loaded.getRoles());
        return loaded;
    }
}
