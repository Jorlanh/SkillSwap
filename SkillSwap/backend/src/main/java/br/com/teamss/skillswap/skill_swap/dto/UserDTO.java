package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;
import java.util.UUID;
import java.util.HashSet;

public class UserDTO {
    @NotNull(message = "userId não pode ser nulo")
    private UUID userId;
    private String username;
    private Set<String> roles = new HashSet<>(); // Inicializar
    private ProfileDTO profile;
    private Set<SkillDTO> skills = new HashSet<>(); // Inicializar
    private String twoFactorSecret;
    private String phoneNumber;
    private String email;
    private String verificationCode;

    // --- START: Accessibility Settings ---
    private boolean librasAvatarEnabled = false; // Valor padrão
    private String preferredTheme = "default";   // Valor padrão
    // --- END: Accessibility Settings ---

    // Construtor original mantido
    public UserDTO(UUID userId, String username, Set<String> roles, ProfileDTO profile, Set<SkillDTO> skills) {
        this.userId = userId;
        this.username = username;
        this.roles = (roles != null) ? roles : new HashSet<>(); // Garantir inicialização
        this.profile = profile;
        this.skills = (skills != null) ? skills : new HashSet<>(); // Garantir inicialização
        // Valores padrão para acessibilidade se usar este construtor
        this.librasAvatarEnabled = false;
        this.preferredTheme = "default";
    }

    // Adicionar um construtor vazio para frameworks como Jackson
    public UserDTO() {
        this.roles = new HashSet<>();
        this.skills = new HashSet<>();
        this.librasAvatarEnabled = false;
        this.preferredTheme = "default";
    }

    // Getters e Setters existentes
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Set<String> getRoles() { return roles; }
    public void setRoles(Set<String> roles) { this.roles = (roles != null) ? roles : new HashSet<>(); } // Garantir inicialização no setter
    public ProfileDTO getProfile() { return profile; }
    public void setProfile(ProfileDTO profile) { this.profile = profile; }
    public Set<SkillDTO> getSkills() { return skills; }
    public void setSkills(Set<SkillDTO> skills) { this.skills = (skills != null) ? skills : new HashSet<>(); } // Garantir inicialização no setter
    public String getTwoFactorSecret() { return twoFactorSecret; }
    public void setTwoFactorSecret(String twoFactorSecret) { this.twoFactorSecret = twoFactorSecret; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getVerificationCode() { return verificationCode; }
    public void setVerificationCode(String verificationCode) { this.verificationCode = verificationCode; }

    // --- START: Accessibility Settings Getters/Setters ---
    public boolean isLibrasAvatarEnabled() {
        return librasAvatarEnabled;
    }

    public void setLibrasAvatarEnabled(boolean librasAvatarEnabled) {
        this.librasAvatarEnabled = librasAvatarEnabled;
    }

    public String getPreferredTheme() {
        return preferredTheme;
    }

    public void setPreferredTheme(String preferredTheme) {
        this.preferredTheme = preferredTheme;
    }
    // --- END: Accessibility Settings Getters/Setters ---
}