package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.SkillDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Skill;
import br.com.teamss.skillswap.skill_swap.model.services.SkillService;
import br.com.teamss.skillswap.skill_swap.model.services.SkillServiceDTO;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SkillServiceDTOImpl implements SkillServiceDTO {

    private final SkillService skillService;

    public SkillServiceDTOImpl(SkillService skillService) {
        this.skillService = skillService;
    }

    @Override
    public SkillDTO toSkillDTO(Skill skill) {
        // Usando o construtor do record SkillDTO com os campos da entidade Skill
        return new SkillDTO(
            skill.getSkillId(),
            skill.getName(),
            skill.getDescription(),
            skill.getCategory(),
            skill.getLevel()
        );
    }

    @Override
    public List<SkillDTO> findAllDTO() {
        return skillService.findAll().stream()
                .map(this::toSkillDTO)
                .collect(Collectors.toList());
    }

    @Override
    public SkillDTO findByIdDTO(Long id) {
        // Lida com Optional<Skill> retornado por skillService.findById
        Optional<Skill> skillOptional = skillService.findById(id);
        return skillOptional.map(this::toSkillDTO)
                .orElseThrow(() -> new RuntimeException("Skill not found with id: " + id));
    }
}