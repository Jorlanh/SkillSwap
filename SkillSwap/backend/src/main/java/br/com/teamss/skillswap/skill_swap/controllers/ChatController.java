package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.ChatMessageRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.ChatMessageResponseDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.ChatMessage;
import br.com.teamss.skillswap.skill_swap.model.services.ChatService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private UserServiceDTO userServiceDTO;

    @GetMapping("/conversations")
    public ResponseEntity<List<Map<String, Object>>> getMyConversations() {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(chatService.getActiveConversations(authenticatedUser.getUserId()));
    }

    @PostMapping("/send")
    public ChatMessageResponseDTO sendMessage(@Valid @RequestBody ChatMessageRequestDTO messageRequest) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        if (!authenticatedUser.getUserId().equals(messageRequest.getSenderId())) {
            throw new AccessDeniedException("Assinatura de usuário inválida.");
        }
        return toResponse(chatService.sendMessage(messageRequest));
    }

    @PostMapping("/send-voice")
    public ChatMessageResponseDTO sendVoiceMessage(@RequestParam("voice") MultipartFile voiceFile, @RequestParam UUID receiverId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        try {
            return toResponse(chatService.sendVoiceMessage(authenticatedUser.getUserId(), receiverId, voiceFile.getBytes()));
        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar áudio: " + e.getMessage());
        }
    }

    @PostMapping("/upload-file")
    public ChatMessageResponseDTO uploadFile(@RequestParam("file") MultipartFile file, @RequestParam UUID receiverId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        try {
            return toResponse(chatService.sendFileMessage(authenticatedUser.getUserId(), receiverId, file.getBytes(), file.getContentType()));
        } catch (IOException e) {
            throw new RuntimeException("Falha no upload de arquivo.");
        }
    }

    @GetMapping("/history/{userId1}/{userId2}")
    public List<ChatMessageResponseDTO> getChatHistory(@PathVariable UUID userId1, @PathVariable UUID userId2) {
        UUID authenticatedUserId = userServiceDTO.getAuthenticatedUser().getUserId();
        if (!authenticatedUserId.equals(userId1) && !authenticatedUserId.equals(userId2)) {
            throw new AccessDeniedException("O histórico solicitado não pertence ao usuário autenticado.");
        }
        return chatService.getChatHistory(userId1, userId2).stream().map(this::toResponse).toList();
    }

    private ChatMessageResponseDTO toResponse(ChatMessage message) {
        ChatMessageResponseDTO response = new ChatMessageResponseDTO();
        response.setMessageId(message.getMessageId());
        response.setSenderId(message.getSender() == null ? null : message.getSender().getUserId());
        response.setReceiverId(message.getReceiver() == null ? null : message.getReceiver().getUserId());
        response.setContent(message.getContent());
        response.setVoiceData(message.getVoiceData());
        response.setFileData(message.getFileData());
        response.setFileType(message.getFileType());
        response.setSentAt(message.getSentAt());
        return response;
    }
}
