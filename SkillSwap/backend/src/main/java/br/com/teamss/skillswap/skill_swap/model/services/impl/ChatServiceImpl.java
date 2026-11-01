package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.ChatMessageRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.ChatMessage;
import br.com.teamss.skillswap.skill_swap.model.entities.Notification;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.exception.InappropriateContentException;
import br.com.teamss.skillswap.skill_swap.model.repositories.ChatRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.NotificationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.ChatService;
import br.com.teamss.skillswap.skill_swap.model.services.ContentModerationService;
import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    @Autowired
    private ChatRepository chatRepository;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ContentModerationService moderationService;

    @Override
    @Transactional
    public ChatMessage sendMessage(ChatMessageRequestDTO messageRequest) {
        if (moderationService.isContentInappropriate(messageRequest.getContent())) {
            throw new InappropriateContentException("A sua mensagem viola as diretrizes da comunidade.");
        }

        User sender = userRepository.findById(messageRequest.getSenderId())
                .orElseThrow(() -> new EntityNotFoundException("Remetente não encontrado"));
        User receiver = userRepository.findById(messageRequest.getReceiverId())
                .orElseThrow(() -> new EntityNotFoundException("Destinatário não encontrado"));

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setContent(messageRequest.getContent());
        message.setSentAt(Instant.now());

        return saveAndNotify(message);
    }

    @Override
    @Transactional
    public ChatMessage sendVoiceMessage(UUID senderId, UUID receiverId, byte[] voiceData) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("Remetente não encontrado"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new EntityNotFoundException("Destinatário não encontrado"));

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setVoiceData(voiceData);
        message.setSentAt(Instant.now());

        return saveAndNotify(message);
    }

    @Override
    @Transactional
    public ChatMessage sendFileMessage(UUID senderId, UUID receiverId, byte[] fileData, String fileType) {
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new EntityNotFoundException("Remetente não encontrado"));
        User receiver = userRepository.findById(receiverId)
                .orElseThrow(() -> new EntityNotFoundException("Destinatário não encontrado"));

        ChatMessage message = new ChatMessage();
        message.setSender(sender);
        message.setReceiver(receiver);
        message.setFileData(fileData);
        message.setFileType(fileType);
        message.setSentAt(Instant.now());
        
        return saveAndNotify(message);
    }

    private ChatMessage saveAndNotify(ChatMessage message) {
        ChatMessage savedMessage = chatRepository.save(message);

        String receiverEmail = savedMessage.getReceiver().getEmail();
        String messageContent = "Você recebeu uma nova mensagem de " + savedMessage.getSender().getUsername() + ".";
        
        emailService.sendNotification(receiverEmail, "Nova Mensagem no SkillSwap", messageContent);
        
        Notification notification = new Notification();
        notification.setUser(savedMessage.getReceiver());
        notification.setMessage(messageContent);
        notification.setSentAt(Instant.now());
        notification.setRead(false);
        notificationRepository.save(notification);

        return savedMessage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getChatHistory(UUID userId1, UUID userId2) {
        return chatRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverId(userId1, userId2, userId1, userId2);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getActiveConversations(UUID userId) {
        List<ChatMessage> allMessages = chatRepository.findAllBySenderIdOrReceiverId(userId);

        Map<UUID, ChatMessage> lastInteractions = new HashMap<>();

        for (ChatMessage msg : allMessages) {
            UUID contactId = msg.getSender().getUserId().equals(userId) 
                             ? msg.getReceiver().getUserId() 
                             : msg.getSender().getUserId();
            
            if (!lastInteractions.containsKey(contactId) || msg.getSentAt().isAfter(lastInteractions.get(contactId).getSentAt())) {
                lastInteractions.put(contactId, msg);
            }
        }

        return lastInteractions.values().stream()
            .sorted(Comparator.comparing(ChatMessage::getSentAt).reversed())
            .map(msg -> {
                User otherUser = msg.getSender().getUserId().equals(userId) ? msg.getReceiver() : msg.getSender();
                Map<String, Object> convo = new HashMap<>();
                convo.put("id", otherUser.getUserId());
                convo.put("lastMessage", msg.getContent() != null ? msg.getContent() : "Arquivo de mídia");
                convo.put("timestamp", msg.getSentAt());
                
                Map<String, Object> userData = new HashMap<>();
                userData.put("id", otherUser.getUserId());
                userData.put("username", otherUser.getUsername());
                // CORREÇÃO: Alterado de getAvatarUrl() para getImageUrl() conforme a sua entidade Profile
                userData.put("avatarUrl", (otherUser.getProfile() != null) ? otherUser.getProfile().getImageUrl() : null);
                
                convo.put("user", userData);
                return convo;
            }).collect(Collectors.toList());
    }
}