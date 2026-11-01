package br.com.teamss.skillswap.skill_swap.model.services;

// Interface para definir os métodos de serviço de e-mail
public interface EmailService {

    void sendNotification(String to, String subject, String body);

    void sendLessonNotification(String toEmail, String lessonDetails);

    void sendPlatformNotification(String message);

    void sendVerificationCode(String to, String code);

    void sendVerificationCodeViaSMS(String phoneNumber, String code); // Adicionado
}