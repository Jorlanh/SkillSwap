package br.com.teamss.skillswap.skill_swap.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Getter
@Setter
public class BanRequestDTO {
    private String reason;
    private Instant expiresAt; // Nulo para permanente
    private String ipAddress;
}