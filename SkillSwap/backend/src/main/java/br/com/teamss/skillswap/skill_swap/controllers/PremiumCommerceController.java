package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.ExtraLessonPurchaseRequest;
import br.com.teamss.skillswap.skill_swap.dto.PremiumPurchaseResponse;
import br.com.teamss.skillswap.skill_swap.model.services.PremiumCommerceService;
import br.com.teamss.skillswap.skill_swap.model.services.PremiumFeatureService;
import br.com.teamss.skillswap.skill_swap.model.services.ChatbotService;
import br.com.teamss.skillswap.skill_swap.dto.ChatbotRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.ChatbotResponseDTO;
import br.com.teamss.skillswap.skill_swap.model.premium.PremiumFeature;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/premium")
@PreAuthorize("isAuthenticated()")
public class PremiumCommerceController {
    private final PremiumCommerceService commerceService;
    private final UserServiceDTO userService;
    private final PremiumFeatureService featureService;
    private final ChatbotService chatbotService;

    public PremiumCommerceController(PremiumCommerceService commerceService, UserServiceDTO userService,
                                     PremiumFeatureService featureService, ChatbotService chatbotService) {
        this.commerceService = commerceService;
        this.userService = userService;
        this.featureService = featureService;
        this.chatbotService = chatbotService;
    }

    @GetMapping("/catalog")
    public ResponseEntity<Map<String, Object>> catalog() { return ResponseEntity.ok(commerceService.catalog()); }

    @GetMapping("/usage/ai")
    public ResponseEntity<Map<String, Object>> aiUsage() {
        return ResponseEntity.ok(featureService.getAiQuota(userService.getAuthenticatedUser().getUserId(), 20));
    }

    @PostMapping("/ai/profile-plan")
    @PremiumFeature(code = "PROFILE_ANALYSIS", cost = 5)
    public ResponseEntity<ChatbotResponseDTO> profilePlan() {
        var user = userService.getAuthenticatedUser();
        String profile = user.getProfile() == null ? "sem perfil preenchido" : user.getProfile().toString();
        String skills = user.getSkills().stream().map(skill -> skill.name() + " (" + skill.level() + ")").toList().toString();
        ChatbotRequestDTO request = new ChatbotRequestDTO();
        request.setMessage("Analise o perfil de aprendizagem do usuário e crie um plano de estudos inclusivo, "
            + "com metas semanais, habilidades prioritárias e próximos passos. Perfil: " + profile + ". Habilidades: " + skills);
        request.setSimplifyResponse(true);
        return ResponseEntity.ok(chatbotService.getChatbotResponse(request));
    }

    @PostMapping("/proposals/{proposalId}/boost")
    public ResponseEntity<PremiumPurchaseResponse> boostProposal(@PathVariable long proposalId) {
        return ResponseEntity.ok(commerceService.boostProposal(userService.getAuthenticatedUser().getUserId(), proposalId));
    }

    @PostMapping("/posts/{postId}/highlight")
    public ResponseEntity<PremiumPurchaseResponse> highlightPost(@PathVariable long postId) {
        return ResponseEntity.ok(commerceService.highlightPost(userService.getAuthenticatedUser().getUserId(), postId));
    }

    @PostMapping("/proposals/{proposalId}/sponsor")
    public ResponseEntity<PremiumPurchaseResponse> sponsorProposal(@PathVariable long proposalId) {
        return ResponseEntity.ok(commerceService.sponsorProposal(userService.getAuthenticatedUser().getUserId(), proposalId));
    }

    @PostMapping("/posts/{postId}/sponsor")
    public ResponseEntity<PremiumPurchaseResponse> sponsorPost(@PathVariable long postId) {
        return ResponseEntity.ok(commerceService.sponsorPost(userService.getAuthenticatedUser().getUserId(), postId));
    }

    @PostMapping("/profile/portfolio-slots")
    public ResponseEntity<PremiumPurchaseResponse> addPortfolioSlot() {
        return ResponseEntity.ok(commerceService.addPortfolioSlot(userService.getAuthenticatedUser().getUserId()));
    }

    @PostMapping("/profile/bio-limit")
    public ResponseEntity<PremiumPurchaseResponse> addBioLimit() {
        return ResponseEntity.ok(commerceService.addBioLimit(userService.getAuthenticatedUser().getUserId()));
    }

    @PostMapping("/profile/post-limit")
    public ResponseEntity<PremiumPurchaseResponse> addPostLimit() {
        return ResponseEntity.ok(commerceService.addPostLimit(userService.getAuthenticatedUser().getUserId()));
    }

    @PostMapping("/lessons/{lessonId}/extra-hours")
    public ResponseEntity<PremiumPurchaseResponse> buyExtraLesson(@PathVariable long lessonId,
                                                                    @Valid @RequestBody ExtraLessonPurchaseRequest request) {
        return ResponseEntity.ok(commerceService.buyExtraLesson(userService.getAuthenticatedUser().getUserId(), lessonId, request.hours()));
    }
}
