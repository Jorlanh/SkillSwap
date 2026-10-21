package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.GenerateAchievementImageRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserAchievementDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.entities.UserAchievement;
import java.util.List;
import java.util.UUID;

public interface AchievementService {
    void checkAndUnlockAchievements(User user);
    UserAchievement generateAchievementImage(GenerateAchievementImageRequestDTO request);
    List<UserAchievementDTO> getUserAchievements(UUID userId);
}