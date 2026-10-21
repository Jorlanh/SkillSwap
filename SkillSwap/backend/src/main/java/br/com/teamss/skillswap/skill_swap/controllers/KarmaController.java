package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.KarmaEventDTO;
import br.com.teamss.skillswap.skill_swap.dto.KarmaRankingDTO;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.KarmaService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/karma")
public class KarmaController {
    private final KarmaService karmaService;
    private final UserServiceDTO userService;
    private final UserRepository userRepository;

    public KarmaController(KarmaService karmaService, UserServiceDTO userService, UserRepository userRepository) {
        this.karmaService = karmaService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        var userId = userService.getAuthenticatedUser().getUserId();
        var events = karmaService.getRecentEvents(userId).stream().map(KarmaEventDTO::from).toList();
        return ResponseEntity.ok(Map.of("points", karmaService.getPoints(userId), "events", events));
    }

    @GetMapping("/top")
    public ResponseEntity<List<KarmaRankingDTO>> top() {
        var ranking = userRepository.findTop20ByOrderByKarmaPointsDesc().stream()
            .map(new java.util.function.Function<br.com.teamss.skillswap.skill_swap.model.entities.User, KarmaRankingDTO>() {
                private int rank = 1;
                @Override public KarmaRankingDTO apply(br.com.teamss.skillswap.skill_swap.model.entities.User user) {
                    return new KarmaRankingDTO(user.getUsername(), user.getName(), user.getKarmaPoints(), rank++);
                }
            }).toList();
        return ResponseEntity.ok(ranking);
    }
}
