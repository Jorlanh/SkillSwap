package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.controllers.PrivacyController;
import br.com.teamss.skillswap.skill_swap.model.entities.GuardianConsent;
import br.com.teamss.skillswap.skill_swap.model.entities.PrivacyConsent;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.GuardianConsentRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.PrivacyConsentRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MinorGuardianService {
    private final GuardianConsentRepository guardianRepository;
    private final PrivacyConsentRepository privacyConsentRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final String frontendUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public MinorGuardianService(GuardianConsentRepository guardianRepository,
                                PrivacyConsentRepository privacyConsentRepository,
                                UserRepository userRepository,
                                EmailService emailService,
                                @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.guardianRepository = guardianRepository;
        this.privacyConsentRepository = privacyConsentRepository;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @Transactional
    public boolean request(User user, String guardianName, String guardianEmail, String relationship, String locale) {
        guardianRepository.findTopByUser_UserIdOrderByRequestedAtDesc(user.getUserId()).ifPresent(previous -> {
            if ("PENDING".equals(previous.getStatus())) {
                previous.setStatus("REVOKED");
                guardianRepository.save(previous);
            }
        });
        GuardianConsent consent = new GuardianConsent();
        consent.setUser(user);
        consent.setGuardianName(guardianName.trim());
        consent.setGuardianEmail(guardianEmail.trim().toLowerCase());
        consent.setRelationship(relationship.trim());
        consent.setPolicyVersion(PrivacyController.CURRENT_POLICY_VERSION);
        consent.setLocale(locale == null || locale.isBlank() ? "pt-BR" : locale.trim());
        consent.setExpiresAt(Instant.now().plus(72, ChronoUnit.HOURS));

        String rawToken = createToken();
        consent.setTokenHash(hash(rawToken));
        guardianRepository.save(consent);

        user.setAccountAgeBand("MINOR");
        user.setMinorAccessStatus("PENDING_GUARDIAN");
        userRepository.save(user);

        // Fragmentos não são enviados ao servidor web, reduzindo a chance de o
        // token aparecer em logs de proxy, histórico de requisições ou Referer.
        String link = frontendUrl.replaceAll("/$", "") + "/consentimento-responsavel#token=" + rawToken;
        try {
            emailService.sendGuardianConsentEmail(consent.getGuardianEmail(), consent.getGuardianName(), user.getName(), link);
            return true;
        } catch (RuntimeException deliveryFailure) {
            // A falha do provedor de e-mail não desfaz o cadastro. O status fica
            // pendente para que a equipe possa reenviar a solicitação.
            return false;
        }
    }

    @Transactional
    public Map<String, Object> confirm(String rawToken, String guardianName, String relationship, boolean consentGranted) {
        GuardianConsent guardianConsent = findUsable(rawToken);
        if (!consentGranted) {
            guardianConsent.setStatus("REVOKED");
            guardianRepository.save(guardianConsent);
            guardianConsent.getUser().setMinorAccessStatus("REVOKED");
            userRepository.save(guardianConsent.getUser());
            return Map.of("approved", false, "status", "REVOKED");
        }
        guardianConsent.setGuardianName(guardianName.trim());
        guardianConsent.setRelationship(relationship.trim());
        guardianConsent.setStatus("APPROVED");
        guardianConsent.setConfirmedAt(Instant.now());
        guardianRepository.save(guardianConsent);

        User user = guardianConsent.getUser();
        user.setMinorAccessStatus("APPROVED");
        userRepository.save(user);

        PrivacyConsent privacyConsent = new PrivacyConsent();
        privacyConsent.setUser(user);
        privacyConsent.setPurpose("MINOR_GUARDIAN_CONSENT");
        privacyConsent.setPolicyVersion(guardianConsent.getPolicyVersion());
        privacyConsent.setGranted(true);
        privacyConsent.setLocale(guardianConsent.getLocale());
        privacyConsent.setSource("guardian-link");
        privacyConsentRepository.save(privacyConsent);
        saveGuardianPolicyConsent(user, guardianConsent, "PRIVACY_POLICY");
        saveGuardianPolicyConsent(user, guardianConsent, "TERMS_OF_SERVICE");
        return Map.of("approved", true, "status", "APPROVED", "minorName", user.getName());
    }

    @Transactional
    public Map<String, Object> publicStatus(String rawToken) {
        GuardianConsent consent = findUsable(rawToken);
        return Map.of(
            "status", consent.getStatus(),
            "minorName", consent.getUser().getName(),
            "relationship", consent.getRelationship(),
            "expiresAt", consent.getExpiresAt()
        );
    }

    @Transactional
    public Map<String, Object> statusFor(User user) {
        if (!"MINOR".equals(user.getAccountAgeBand())) {
            return Map.of("isMinor", false, "status", "NOT_APPLICABLE");
        }
        return guardianRepository.findTopByUser_UserIdOrderByRequestedAtDesc(user.getUserId())
            .map(consent -> Map.of(
                "isMinor", true,
                "status", user.getMinorAccessStatus(),
                "guardianConsentStatus", consent.getStatus(),
                "expiresAt", consent.getExpiresAt()
            ))
            .orElseGet(() -> Map.of("isMinor", true, "status", user.getMinorAccessStatus()));
    }

    private GuardianConsent findUsable(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) throw new IllegalArgumentException("Token de responsável ausente.");
        GuardianConsent consent = guardianRepository.findByTokenHash(hash(rawToken.trim()))
            .orElseThrow(() -> new IllegalArgumentException("Link de confirmação inválido ou expirado."));
        if ("PENDING".equals(consent.getStatus()) && consent.getExpiresAt().isBefore(Instant.now())) {
            consent.setStatus("EXPIRED");
            guardianRepository.save(consent);
            throw new IllegalArgumentException("Link de confirmação expirado.");
        }
        if (!"PENDING".equals(consent.getStatus())) {
            throw new IllegalArgumentException("Este link já foi utilizado ou revogado.");
        }
        return consent;
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private void saveGuardianPolicyConsent(User user, GuardianConsent guardianConsent, String purpose) {
        PrivacyConsent consent = new PrivacyConsent();
        consent.setUser(user);
        consent.setPurpose(purpose);
        consent.setPolicyVersion(guardianConsent.getPolicyVersion());
        consent.setGranted(true);
        consent.setLocale(guardianConsent.getLocale());
        consent.setSource("guardian-link");
        privacyConsentRepository.save(consent);
    }

    private static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) result.append(String.format("%02x", value));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 indisponível", exception);
        }
    }
}
