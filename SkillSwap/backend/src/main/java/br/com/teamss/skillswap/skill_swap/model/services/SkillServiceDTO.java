package br.com.teamss.skillswap.skill_swap.model.services;

import java.util.List;

import br.com.teamss.skillswap.skill_swap.dto.SkillDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Skill;

public interface SkillServiceDTO {
    SkillDTO toSkillDTO(Skill skill);
    List<SkillDTO> findAllDTO();
    SkillDTO findByIdDTO(Long id);
}
