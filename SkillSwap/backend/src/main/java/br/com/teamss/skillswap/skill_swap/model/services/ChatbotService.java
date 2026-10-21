package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.ChatbotRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.ChatbotResponseDTO;

/**
 * Interface defining the contract for the Chatbot service.
 */
public interface ChatbotService {

    /**
     * Processes a user request and returns a response from the underlying AI model.
     * Handles potential text simplification based on the request.
     *
     * @param requestDTO The user's request, including the message and options.
     * @return A DTO containing the chatbot's response.
     */
    ChatbotResponseDTO getChatbotResponse(ChatbotRequestDTO requestDTO);

}