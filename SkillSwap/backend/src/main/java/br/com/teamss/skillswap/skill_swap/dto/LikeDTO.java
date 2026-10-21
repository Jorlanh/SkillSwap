package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;

// DTO para exibir uma curtida de forma segura
public class LikeDTO {

    private UserSummaryDTO user;
    private Instant createdAt;

    // Construtor, Getters e Setters
    public LikeDTO(UserSummaryDTO user, Instant createdAt) {
        this.user = user;
        this.createdAt = createdAt;
    }

    public UserSummaryDTO getUser() {
        return user;
    }

    public void setUser(UserSummaryDTO user) {
        this.user = user;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}