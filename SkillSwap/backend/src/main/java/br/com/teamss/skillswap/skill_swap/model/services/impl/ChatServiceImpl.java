package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.ChatMessageRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.ChatMessage;
import br.com.teamss.skillswap.skill_swap.model.entities.Notification;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.exception.InappropriateContentException;
import br.com.teamss.skillswap.skill_swap.model.repositories.ChatRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.NotificationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProposalRepository;
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
    @Autowired
    private ProposalRepository proposalRepository;

    @Override
    @Transactional
    public ChatMessage sendMessage(ChatMessageRequestDTO messageRequest) {
        ensureAcceptedProposal(messageRequest.getSenderId(), messageRequest.getReceiverId());
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
        ensureAcceptedProposal(senderId, receiverId);
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
        ensureAcceptedProposal(senderId, receiverId);
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

        Notification notification = new Notification();
        notification.setUser(savedMessage.getReceiver());
        notification.setMessage(messageContent);
        notification.setSentAt(Instant.now());
        notification.setRead(false);
        notificationRepository.save(notification);

        // A mensagem continua disponível mesmo quando o SMTP do ambiente ainda
        // não foi configurado. O e-mail é uma camada adicional de aviso, não o
        // mecanismo de persistência do chat.
        try {
            emailService.sendNotification(receiverEmail, "Nova Mensagem no SkillSwap", messageContent);
        } catch (RuntimeException exception) {
            System.err.println("Aviso: não foi possível enviar o e-mail da mensagem: " + exception.getMessage());
        }

        return savedMessage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatMessage> getChatHistory(UUID userId1, UUID userId2) {
        ensureAcceptedProposal(userId1, userId2);
        return chatRepository.findBySenderIdAndReceiverIdOrSenderIdAndReceiverId(userId1, userId2);
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
            
            if (proposalRepository.existsAcceptedBetween(userId, contactId)
                    && (!lastInteractions.containsKey(contactId) || msg.getSentAt().isAfter(lastInteractions.get(contactId).getSentAt()))) {
                lastInteractions.put(contactId, msg);
            }
        }

        // Uma conversa também deve aparecer logo após a proposta ser aceita,
        // mesmo antes da primeira mensagem. Assim o usuário não precisa criar
        // uma conversa falsa no navegador para iniciar o contato permitido.
        Map<UUID, Map<String, Object>> conversations = new HashMap<>();
        lastInteractions.forEach((contactId, message) -> {
            User otherUser = message.getSender().getUserId().equals(userId)
                    ? message.getReceiver()
                    : message.getSender();
            conversations.put(contactId, conversationMap(otherUser,
                    message.getContent() != null ? message.getContent() : "Arquivo de mídia",
                    message.getSentAt()));
        });

        proposalRepository.findBySenderIdOrReceiverId(userId).stream()
                .filter(proposal -> "ACCEPTED".equalsIgnoreCase(proposal.getStatus())
                        || "COMPLETED".equalsIgnoreCase(proposal.getStatus()))
                .forEach(proposal -> {
                    User otherUser = proposal.getSender().getUserId().equals(userId)
                            ? proposal.getReceiver()
                            : proposal.getSender();
                    conversations.putIfAbsent(otherUser.getUserId(),
                            conversationMap(otherUser, "Conversa liberada", proposal.getUpdatedAt()));
                });

        return conversations.values().stream()
                .sorted(Comparator.comparing(
                        conversation -> (Instant) conversation.get("timestamp"),
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
    }

    private Map<String, Object> conversationMap(User otherUser, String lastMessage, Instant timestamp) {
        Map<String, Object> conversation = new HashMap<>();
        conversation.put("id", otherUser.getUserId());
        conversation.put("lastMessage", lastMessage);
        conversation.put("timestamp", timestamp);

        Map<String, Object> userData = new HashMap<>();
        userData.put("id", otherUser.getUserId());
        userData.put("username", otherUser.getUsername());
        userData.put("name", otherUser.getName());
        userData.put("avatarUrl", otherUser.getProfile() == null
                ? null
                : otherUser.getProfile().getImageUrl());
        conversation.put("user", userData);
        return conversation;
    }

    private void ensureAcceptedProposal(UUID userId1, UUID userId2) {
        if (userId1 == null || userId2 == null || !proposalRepository.existsAcceptedBetween(userId1, userId2)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "A conversa só fica disponível depois que uma proposta é aceita.");
        }
    }
}
