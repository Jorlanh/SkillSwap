package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    boolean existsByUser_UserIdAndAchievement_Id(UUID userId, Long achievementId);
    List<UserAchievement> findByUser_UserId(UUID userId);
}