package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.ModerationSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationSettingsRepository extends JpaRepository<ModerationSettings, Integer> {
}
