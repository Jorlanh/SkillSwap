package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class UserIdDTO {
    @NotNull(message = "userId não pode ser nulo")
    private UUID userId;

    // Getters e Setters
    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }
}