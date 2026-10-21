package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.SkillDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Skill;
import br.com.teamss.skillswap.skill_swap.model.services.SkillService;
import br.com.teamss.skillswap.skill_swap.model.services.SkillServiceDTO;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
public class SkillController {

    private final SkillService skillService;
    private final SkillServiceDTO skillServiceDTO;

    public SkillController(SkillService skillService, SkillServiceDTO skillServiceDTO) {
        this.skillService = skillService;
        this.skillServiceDTO = skillServiceDTO;
    }

    @GetMapping
    public ResponseEntity<List<SkillDTO>> getAllSkills() {
        return ResponseEntity.ok(skillServiceDTO.findAllDTO());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillDTO> getSkillById(@PathVariable Long id) {
        return ResponseEntity.ok(skillServiceDTO.findByIdDTO(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SkillDTO> createSkill(@RequestBody Skill skill) {
        Skill savedSkill = skillService.save(skill);
        SkillDTO skillDTO = skillServiceDTO.toSkillDTO(savedSkill);
        return ResponseEntity.ok(skillDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SkillDTO> updateSkill(@PathVariable Long id, @RequestBody Skill skill) {
        skill.setSkillId(id);
        Skill updatedSkill = skillService.save(skill);
        SkillDTO skillDTO = skillServiceDTO.toSkillDTO(updatedSkill);
        return ResponseEntity.ok(skillDTO);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSkill(@PathVariable Long id) {
        skillService.delete(id);
        return ResponseEntity.noContent().build();
    }
}