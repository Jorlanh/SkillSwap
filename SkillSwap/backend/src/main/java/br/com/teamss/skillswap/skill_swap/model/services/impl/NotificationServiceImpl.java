package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.NotificationDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Notification;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.NotificationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.NotificationService;

// IMPORTS DO FIREBASE ADICIONADOS
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<NotificationDTO> getUnreadNotifications(UUID userId) {
        List<Notification> notifications = notificationRepository.findByUserUserIdAndReadFalse(userId);
        return notifications.stream()
                .map(notification -> new NotificationDTO(
                        notification.getNotificationId(),
                        notification.getMessage(),
                        notification.getSentAt(),
                        notification.isRead()
                ))
                .collect(Collectors.toList());
    }

    @Override
    public void markAsRead(Long notificationId, UUID userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificação não encontrada!"));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new AccessDeniedException("Você não tem permissão para marcar esta notificação como lida.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void createNotification(UUID userId, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        // 1. SALVAR NO BANCO POSTGRES (Para histórico na plataforma)
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setMessage(message);
        notification.setSentAt(Instant.now());
        notification.setRead(false);
        notificationRepository.save(notification);

        // 2. DISPARAR PUSH REAL (Para o navegador/celular do usuário)
        // Certifique-se de que o campo fcmToken existe na sua entidade User
        String deviceToken = user.getFcmToken(); 
        
        if (deviceToken != null && !deviceToken.isEmpty()) {
            try {
                Message firebaseMessage = Message.builder()
                    .setToken(deviceToken)
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                        .setTitle("SkillSwapHub")
                        .setBody(message)
                        .build())
                    .build();

                FirebaseMessaging.getInstance().sendAsync(firebaseMessage);
            } catch (Exception e) {
                // Log de erro silencioso para não travar o fluxo do sistema
                System.err.println("Erro ao enviar Push: " + e.getMessage());
            }
        }
    }
}