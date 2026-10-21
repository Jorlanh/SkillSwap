package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.model.entities.Skill;
import java.util.List;
import java.util.Optional;

public interface SkillService {
    Skill save(Skill skill);
    Optional<Skill> findById(Long id);
    List<Skill> findAll();
    void delete(Long id);
}