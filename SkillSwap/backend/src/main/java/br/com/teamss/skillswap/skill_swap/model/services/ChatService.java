package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.ChatMessageRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.ChatMessage;
import java.util.List;
import java.util.UUID;
import java.util.Map; // IMPORT ESSENCIAL ADICIONADO

public interface ChatService {
    ChatMessage sendMessage(ChatMessageRequestDTO messageRequest);
    ChatMessage sendVoiceMessage(UUID senderId, UUID receiverId, byte[] voiceData);
    ChatMessage sendFileMessage(UUID senderId, UUID receiverId, byte[] fileData, String fileType);
    List<ChatMessage> getChatHistory(UUID userId1, UUID userId2);
    
    // Método para buscar o resumo das conversas para a barra lateral do Front
    List<Map<String, Object>> getActiveConversations(UUID userId);
}