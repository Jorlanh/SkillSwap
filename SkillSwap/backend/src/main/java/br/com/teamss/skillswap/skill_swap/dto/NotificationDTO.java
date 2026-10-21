package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;

// DTO para exibir uma Notificação de forma segura.
public class NotificationDTO {

    private Long notificationId;
    private String message;
    private Instant sentAt;
    private boolean read;

    // Construtores, Getters e Setters
    public NotificationDTO(Long notificationId, String message, Instant sentAt, boolean read) {
        this.notificationId = notificationId;
        this.message = message;
        this.sentAt = sentAt;
        this.read = read;
    }

    public Long getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(Long notificationId) {
        this.notificationId = notificationId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant sentAt) {
        this.sentAt = sentAt;
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }
}