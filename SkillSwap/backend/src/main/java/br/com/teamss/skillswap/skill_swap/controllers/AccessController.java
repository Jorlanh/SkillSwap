package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.AccessRecommendationDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.AccessOpportunity;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.AccessOpportunityRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/access")
public class AccessController {
    private final AccessOpportunityRepository opportunityRepository;
    private final UserRepository userRepository;
    private final UserServiceDTO userServiceDTO;

    public AccessController(AccessOpportunityRepository opportunityRepository, UserRepository userRepository, UserServiceDTO userServiceDTO) {
        this.opportunityRepository = opportunityRepository;
        this.userRepository = userRepository;
        this.userServiceDTO = userServiceDTO;
    }

    @GetMapping("/overview")
    public Map<String, String> overview() {
        return Map.of(
            "profileStatus", "habilidades e preferências locais",
            "recommendationStrategy", "pontuação por correspondência de habilidade e suporte acessível",
            "privacy", "dados mínimos; nenhuma decisão automatizada sem explicação"
        );
    }

    @GetMapping("/opportunities")
    public List<AccessOpportunity> opportunities() {
        return opportunityRepository.findTop30ByStatusOrderByCreatedAtDesc("OPEN");
    }

    @GetMapping("/recommendations")
    @Transactional(readOnly = true)
    public List<AccessRecommendationDTO> recommendations() {
        UserDTO current = userServiceDTO.getAuthenticatedUser();
        User user = userRepository.findById(current.getUserId()).orElseThrow();
        Set<String> skills = user.getSkills().stream()
            .map(skill -> safe(skill.getName()).toLowerCase(Locale.ROOT))
            .filter(value -> !value.isBlank())
            .collect(Collectors.toSet());
        String profileText = user.getProfile() == null ? "" : String.join(" ",
            safe(user.getProfile().getInterests()), safe(user.getProfile().getDescription())).toLowerCase(Locale.ROOT);

        return opportunityRepository.findTop30ByStatusOrderByCreatedAtDesc("OPEN").stream()
            .map(opportunity -> toRecommendation(opportunity, skills, profileText))
            .sorted(Comparator.comparingInt(AccessRecommendationDTO::score).reversed())
            .limit(10)
            .toList();
    }

    @PostMapping("/opportunities")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AccessOpportunity> create(@Valid @RequestBody OpportunityRequest request) {
        AccessOpportunity opportunity = new AccessOpportunity();
        opportunity.setOrganizationName(request.organizationName());
        opportunity.setTitle(request.title());
        opportunity.setDescription(request.description());
        opportunity.setRequiredSkills(request.requiredSkills());
        opportunity.setRemote(request.remote());
        opportunity.setAccessibleSupport(request.accessibleSupport());
        opportunity.setLocation(request.location());
        return ResponseEntity.ok(opportunityRepository.save(opportunity));
    }

    private AccessRecommendationDTO toRecommendation(AccessOpportunity opportunity, Set<String> skills, String profileText) {
        List<String> matches = Arrays.stream(opportunity.getRequiredSkills().split(","))
            .map(value -> value.trim().toLowerCase(Locale.ROOT))
            .filter(value -> skills.stream().anyMatch(skill -> skill.contains(value) || value.contains(skill)) || profileText.contains(value))
            .toList();
        int score = Math.min(99, 20 + matches.size() * 25 + (opportunity.isAccessibleSupport() ? 20 : 0) + (opportunity.isRemote() ? 5 : 0));
        String reason = matches.isEmpty()
            ? "compatibilidade inicial; adicione habilidades ou interesses para refinar a recomendação"
            : "habilidades/interesses compatíveis: " + String.join(", ", matches) + (opportunity.isAccessibleSupport() ? "; oferece apoio de acessibilidade" : "");
        return new AccessRecommendationDTO(opportunity.getOpportunityId(), opportunity.getOrganizationName(), opportunity.getTitle(), opportunity.getDescription(), opportunity.isRemote(), opportunity.isAccessibleSupport(), score, reason);
    }

    private static String safe(String value) { return value == null ? "" : value; }

    public record OpportunityRequest(
        @NotBlank @Size(max = 160) String organizationName,
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotBlank @Size(max = 1000) String requiredSkills,
        boolean remote,
        boolean accessibleSupport,
        @Size(max = 120) String location
    ) {}
}
