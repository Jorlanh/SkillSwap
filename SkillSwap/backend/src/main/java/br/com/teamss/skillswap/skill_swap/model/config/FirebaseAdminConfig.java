package br.com.teamss.skillswap.skill_swap.model.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

/** Initializes Firebase Admin only from a deployment secret. */
@Configuration
public class FirebaseAdminConfig {

    private static final Logger logger = LoggerFactory.getLogger(FirebaseAdminConfig.class);

    @Value("${firebase.service-account-json:}")
    private String serviceAccountJson;

    @Value("${firebase.project-id:}")
    private String projectId;

    @PostConstruct
    public void initialize() {
        if (serviceAccountJson == null || serviceAccountJson.isBlank()) {
            logger.warn("Firebase Admin não foi inicializado: FIREBASE_SERVICE_ACCOUNT_JSON não foi configurada.");
            return;
        }
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }

        try (ByteArrayInputStream input = new ByteArrayInputStream(
                serviceAccountJson.getBytes(StandardCharsets.UTF_8))) {
            FirebaseOptions.Builder builder = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(input));
            if (projectId != null && !projectId.isBlank()) {
                builder.setProjectId(projectId);
            }
            FirebaseApp.initializeApp(builder.build());
            logger.info("Firebase Admin inicializado para notificações push.");
        } catch (Exception exception) {
            // A plataforma continua iniciando; e-mail e notificações internas não dependem do push.
            logger.warn("Não foi possível inicializar o Firebase Admin: {}", exception.getMessage());
        }
    }
}
