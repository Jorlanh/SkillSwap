package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.RatingRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserRankingDTO;
import br.com.teamss.skillswap.skill_swap.model.services.RankingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class RankingController {

    private final RankingService rankingService;

    public RankingController(RankingService rankingService) {
        this.rankingService = rankingService;
    }

    @PostMapping("/ratings")
    public ResponseEntity<?> createRating(@Valid @RequestBody RatingRequestDTO ratingRequestDTO) {
        rankingService.submitRating(ratingRequestDTO);
        return ResponseEntity.ok().body("Avaliação submetida com sucesso.");
    }

    @GetMapping("/rankings")
    public ResponseEntity<List<UserRankingDTO>> getRankings(@RequestParam Optional<Long> skillId) {
        List<UserRankingDTO> rankings = rankingService.getRankings(skillId);
        return ResponseEntity.ok(rankings);
    }
}