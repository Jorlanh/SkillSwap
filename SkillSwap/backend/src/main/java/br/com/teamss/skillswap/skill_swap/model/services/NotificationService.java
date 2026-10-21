package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.NotificationDTO;
import java.util.List;
import java.util.UUID;

public interface NotificationService {
    List<NotificationDTO> getUnreadNotifications(UUID userId);
    void markAsRead(Long notificationId, UUID userId); // Assinatura alterada
    void createNotification(UUID userId, String message);
}