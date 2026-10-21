package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.GenerateAchievementImageRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserAchievementDTO;
import br.com.teamss.skillswap.skill_swap.model.services.AchievementService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @PostMapping("/generate-image")
    public ResponseEntity<?> generateImage(@Valid @RequestBody GenerateAchievementImageRequestDTO request) {
        achievementService.generateAchievementImage(request);
        return ResponseEntity.ok().body("Imagem da conquista gerada e associada ao seu perfil!");
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<UserAchievementDTO>> getUserAchievements(@PathVariable UUID userId) {
        return ResponseEntity.ok(achievementService.getUserAchievements(userId));
    }
}