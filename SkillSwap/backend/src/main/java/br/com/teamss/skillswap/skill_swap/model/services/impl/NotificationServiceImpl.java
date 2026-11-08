package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.NotificationDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Notification;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.NotificationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import br.com.teamss.skillswap.skill_swap.model.services.NotificationService;

// IMPORTS DO FIREBASE ADICIONADOS
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${notifications.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notifications.push.enabled:true}")
    private boolean pushEnabled;

    @Autowired
    public NotificationServiceImpl(NotificationRepository notificationRepository,
                                   UserRepository userRepository,
                                   EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @Override
    public List<NotificationDTO> getUnreadNotifications(UUID userId) {
        return toDTOs(notificationRepository.findByUserUserIdAndReadFalse(userId));
    }

    @Override
    public List<NotificationDTO> getAllNotifications(UUID userId) {
        return toDTOs(notificationRepository.findByUserUserIdOrderBySentAtDesc(userId));
    }

    private List<NotificationDTO> toDTOs(List<Notification> notifications) {
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

        // 2. E-MAIL TRANSACIONAL (Brevo por API; SMTP como fallback)
        if (emailEnabled && user.getEmail() != null && !user.getEmail().isBlank()) {
            try {
                emailService.sendNotification(user.getEmail(), "SkillSwap - Nova notificação", message);
            } catch (RuntimeException exception) {
                logger.warn("Notificação interna salva, mas o e-mail não foi enviado para {}: {}",
                        user.getEmail(), exception.getMessage());
            }
        }

        // 3. PUSH REAL (Firebase Cloud Messaging)
        String deviceToken = user.getFcmToken();
        if (pushEnabled && deviceToken != null && !deviceToken.isBlank()) {
            try {
                if (com.google.firebase.FirebaseApp.getApps().isEmpty()) {
                    logger.warn("Push não enviado para {}: Firebase Admin não está configurado.", user.getUserId());
                    return;
                }
                Message firebaseMessage = Message.builder()
                        .setToken(deviceToken)
                        .setNotification(com.google.firebase.messaging.Notification.builder()
                                .setTitle("SkillSwap")
                                .setBody(message)
                                .build())
                        .build();
                FirebaseMessaging.getInstance().sendAsync(firebaseMessage);
            } catch (Exception e) {
                logger.warn("Não foi possível enviar push para {}: {}", user.getUserId(), e.getMessage());
            }
        }
    }
}
