package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.AccessibilityPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AccessibilityPreferencesRepository extends JpaRepository<AccessibilityPreferences, UUID> {
}
