package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.ChatbotRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.ChatbotResponseDTO;
import br.com.teamss.skillswap.skill_swap.model.services.ChatbotService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    /**
     * Endpoint for users to interact with the chatbot.
     * Requires authentication.
     * @param requestDTO The user's message and options.
     * @return The chatbot's response.
     */
    @PostMapping("/ask")
    @PreAuthorize("isAuthenticated()") // Ensure only logged-in users can access the chatbot
    public ResponseEntity<ChatbotResponseDTO> askChatbot(@Valid @RequestBody ChatbotRequestDTO requestDTO) {
        ChatbotResponseDTO response = chatbotService.getChatbotResponse(requestDTO);
        return ResponseEntity.ok(response);
    }
}
