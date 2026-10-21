package br.com.teamss.skillswap.skill_swap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.Instant;

@Getter
@AllArgsConstructor
public class UserAchievementDTO {
    private String name;
    private String description;
    private String imageUrl;
    private Instant unlockedAt;
}