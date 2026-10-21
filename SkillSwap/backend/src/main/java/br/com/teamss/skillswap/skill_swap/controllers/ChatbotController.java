package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.ChatbotRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.ChatbotResponseDTO;
import br.com.teamss.skillswap.skill_swap.model.services.ChatbotService;
import br.com.teamss.skillswap.skill_swap.model.services.PrivacyConsentService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import br.com.teamss.skillswap.skill_swap.model.services.MinorSafetyService;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.premium.PremiumFeature;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotController {

    @Autowired
    private ChatbotService chatbotService;

    @Autowired
    private PrivacyConsentService privacyConsentService;

    @Autowired
    private UserServiceDTO userServiceDTO;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MinorSafetyService minorSafetyService;

    /**
     * Endpoint for users to interact with the chatbot.
     * Requires authentication.
     * @param requestDTO The user's message and options.
     * @return The chatbot's response.
     */
    @PostMapping("/ask")
    @PreAuthorize("isAuthenticated()") // Ensure only logged-in users can access the chatbot
    @PremiumFeature(code = "CHATBOT_QUERY", cost = 1, freeQuota = 20)
    public ResponseEntity<ChatbotResponseDTO> askChatbot(@Valid @RequestBody ChatbotRequestDTO requestDTO) {
        var userId = userServiceDTO.getAuthenticatedUser().getUserId();
        User user = userRepository.findById(userId).orElseThrow();
        if (minorSafetyService.isMinor(user) && !"APPROVED".equalsIgnoreCase(user.getMinorAccessStatus())) {
            return ResponseEntity.<ChatbotResponseDTO>status(HttpStatus.PRECONDITION_REQUIRED).build();
        }
        if (!privacyConsentService.isGranted(userId, "OPTIONAL_AI")) {
            return ResponseEntity.<ChatbotResponseDTO>status(HttpStatus.PRECONDITION_REQUIRED).build();
        }
        ChatbotResponseDTO response = chatbotService.getChatbotResponse(requestDTO);
        return ResponseEntity.ok(response);
    }
}
