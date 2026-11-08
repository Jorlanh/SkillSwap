package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage; 
import org.springframework.mail.javamail.JavaMailSender; 
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Pattern;

@Service
public class EmailServiceImpl implements EmailService {

    private static final Pattern SIMPLE_EMAIL = Pattern.compile(
        "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"
    );

    private final JavaMailSender mailSender;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    // Obtém o email remetente configurado no application.properties (spring.mail.username)
    @Value("${spring.mail.username:}")
    private String mailFrom;

    @Value("${brevo.api-key:}")
    private String brevoApiKey;

    @Value("${brevo.api-url:https://api.brevo.com/v3/smtp/email}")
    private String brevoApiUrl;

    @Value("${brevo.sender-email:}")
    private String brevoSenderEmail;

    @Value("${brevo.sender-name:SkillSwap}")
    private String brevoSenderName;

    @Value("${twilio.account.sid:}")
    private String twilioAccountSid;

    @Value("${twilio.auth.token:}")
    private String twilioAuthToken;
    
    @Value("${twilio.phone.number:}")
    private String twilioPhoneNumber;

    @Autowired
    public EmailServiceImpl(JavaMailSender mailSender, ObjectMapper objectMapper) {
        this.mailSender = mailSender;
        this.objectMapper = objectMapper;
    }

    // Método de envio de notificação usando Spring Mail
    @Override
    public void sendNotification(String to, String subject, String body) {
        String normalizedTo = to == null ? "" : to.trim();
        String normalizedFrom = mailFrom == null ? "" : mailFrom.trim();

        if (normalizedTo.isBlank() || !SIMPLE_EMAIL.matcher(normalizedTo).matches()) {
            throw new IllegalArgumentException("Destinatário de e-mail inválido");
        }

        try {
            if (brevoApiKey != null && !brevoApiKey.isBlank()) {
                sendViaBrevo(normalizedTo, subject, body);
            } else {
                SimpleMailMessage message = new SimpleMailMessage();
                if (!normalizedFrom.isBlank() && SIMPLE_EMAIL.matcher(normalizedFrom).matches()) {
                    message.setFrom(normalizedFrom);
                }
                message.setTo(normalizedTo);
                message.setSubject(subject == null ? "SkillSwap" : subject.trim());
                message.setText(body == null ? "" : body);
                mailSender.send(message);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Falha ao enviar e-mail de notificação: " + ex.getMessage(), ex);
        }
    }

    private void sendViaBrevo(String to, String subject, String body) throws Exception {
        String sender = brevoSenderEmail == null || brevoSenderEmail.isBlank()
                ? mailFrom
                : brevoSenderEmail.trim();
        if (sender == null || !SIMPLE_EMAIL.matcher(sender).matches()) {
            throw new IllegalStateException("BREVO_SENDER_EMAIL precisa ser um remetente verificado no Brevo.");
        }

        ObjectNode payload = objectMapper.createObjectNode();
        ObjectNode senderNode = payload.putObject("sender");
        senderNode.put("email", sender);
        senderNode.put("name", brevoSenderName == null || brevoSenderName.isBlank() ? "SkillSwap" : brevoSenderName);
        ArrayNode recipients = payload.putArray("to");
        recipients.addObject().put("email", to);
        payload.put("subject", subject == null || subject.isBlank() ? "SkillSwap" : subject.trim());
        payload.put("textContent", body == null ? "" : body);

        HttpRequest request = HttpRequest.newBuilder(URI.create(brevoApiUrl))
                .header("accept", "application/json")
                .header("api-key", brevoApiKey.trim())
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Brevo respondeu HTTP " + response.statusCode());
        }
    }

    @Override
    public void sendVerificationCode(String to, String code) {
        String subject = "SkillSwap - Código de Verificação";
        String body = "Seu código de verificação para o SkillSwap é: " + code + "\nDigite este código na tela de verificação para ativar sua conta.";
        sendNotification(to, subject, body);
    }

    @Override
    public void sendVerificationCodeViaSMS(String phoneNumber, String code) {
        Twilio.init(twilioAccountSid, twilioAuthToken);
        String messageBody = "Seu código de verificação para o SkillSwap é: " + code;
        Message.creator(
            new com.twilio.type.PhoneNumber(phoneNumber),
            new com.twilio.type.PhoneNumber(twilioPhoneNumber),
            messageBody
        ).create();
    }

    @Override
    public void sendLessonNotification(String toEmail, String lessonDetails) {
        sendNotification(toEmail, "Lembrete de Aula", "Lembrete de Aula: " + lessonDetails);
    }

    @Override
    public void sendPlatformNotification(String message) {
        // NOTA DE REFATORAÇÃO: O EmailService não gerencia mais WebSockets.
        // As notificações de plataforma em tempo real devem ser tratadas pelo ChatWebSocketHandler
        // ou por um serviço de mensageria dedicado.
        System.out.println("[EmailService] Log - Solicitação de notificação interna na plataforma: " + message);
    }
}
