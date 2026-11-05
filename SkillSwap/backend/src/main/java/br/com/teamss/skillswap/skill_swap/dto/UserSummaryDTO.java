package br.com.teamss.skillswap.skill_swap.dto;

import java.util.UUID;

public class UserSummaryDTO {

    private String username;
    private String name;
    private boolean verifiedBadge; // NOVO CAMPO
    private UUID userId;
    private String avatarUrl;

    public UserSummaryDTO(String username, String name, boolean verifiedBadge) {
        this.username = username;
        this.name = name;
        this.verifiedBadge = verifiedBadge;
    }

    public UserSummaryDTO(String username, String name) {
        this.username = username;
        this.name = name;
        this.verifiedBadge = false; // Valor padrão
    }
    
    // Construtor antigo mantido para retrocompatibilidade interna, se necessário, mas não para exposição.
    public UserSummaryDTO(UUID userId, String username, String name) {
        this.userId = userId;
        this.username = username;
        this.name = name;
        this.verifiedBadge = false;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public boolean isVerifiedBadge() { return verifiedBadge; }
    public void setVerifiedBadge(boolean verifiedBadge) { this.verifiedBadge = verifiedBadge; }
}
