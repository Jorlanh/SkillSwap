package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.PrivacyConsentDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.ChatMessage;
import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.PrivacyConsent;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.AccessLogRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ChatRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.LessonRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.GuardianConsentRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.PostRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.PrivacyConsentRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProfileRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import br.com.teamss.skillswap.skill_swap.model.services.MinorGuardianService;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.sql.Date;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints de transparência e direitos do titular. */
@RestController
@RequestMapping("/api/privacy")
public class PrivacyController {
    public static final String CURRENT_POLICY_VERSION = "2026-10-21";
    private static final Set<String> PURPOSES = Set.of(
        "PRIVACY_POLICY", "TERMS_OF_SERVICE", "OPTIONAL_AI", "OPTIONAL_CALENDAR",
        "OPTIONAL_VIDEO", "OPTIONAL_NOTIFICATIONS", "MINOR_GUARDIAN_CONSENT"
    );

    private final UserServiceDTO userServiceDTO;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final PrivacyConsentRepository consentRepository;
    private final AccessLogRepository accessLogRepository;
    private final ChatRepository chatRepository;
    private final PostRepository postRepository;
    private final LessonRepository lessonRepository;
    private final MinorGuardianService minorGuardianService;
    private final GuardianConsentRepository guardianConsentRepository;

    @Value("${privacy.controller.name:Responsável pelo tratamento a definir}")
    private String controllerName;

    @Value("${privacy.controller.contact:privacidade a definir antes da publicação}")
    private String controllerContact;

    @Value("${privacy.dpo.name:Encarregado/DPO a definir}")
    private String dpoName;

    @Value("${privacy.dpo.contact:encarregado a definir antes da publicação}")
    private String dpoContact;

    @Value("${privacy.jurisdiction:Brasil}")
    private String jurisdiction;

    public PrivacyController(UserServiceDTO userServiceDTO, UserRepository userRepository,
                             ProfileRepository profileRepository, PrivacyConsentRepository consentRepository,
                             AccessLogRepository accessLogRepository, ChatRepository chatRepository,
                             PostRepository postRepository, LessonRepository lessonRepository,
                             MinorGuardianService minorGuardianService,
                             GuardianConsentRepository guardianConsentRepository) {
        this.userServiceDTO = userServiceDTO;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.consentRepository = consentRepository;
        this.accessLogRepository = accessLogRepository;
        this.chatRepository = chatRepository;
        this.postRepository = postRepository;
        this.lessonRepository = lessonRepository;
        this.minorGuardianService = minorGuardianService;
        this.guardianConsentRepository = guardianConsentRepository;
    }

    @GetMapping("/policy")
    public Map<String, Object> policy() {
        return Map.of(
            "name", "SkillSwap",
            "version", CURRENT_POLICY_VERSION,
            "languages", List.of("pt-BR", "en"),
            "controllerName", controllerName,
            "controllerContact", controllerContact,
            "dpoName", dpoName,
            "dpoContact", dpoContact,
            "jurisdiction", jurisdiction,
            "minorPolicy", "Pessoas menores de 18 anos podem utilizar a plataforma. O cadastro solicita a confirmação de um responsável por link seguro.",
            "optionalPurposes", List.of("OPTIONAL_AI", "OPTIONAL_CALENDAR", "OPTIONAL_VIDEO", "OPTIONAL_NOTIFICATIONS")
        );
    }

    @GetMapping("/minor-policy")
    public Map<String, Object> minorPolicy() {
        return Map.of(
            "minimumAge", 0,
            "guardianRequiredUnderAge", 18,
            "supported", true,
            "accessWhilePending", true,
            "message", "Pessoas menores de 18 anos podem criar e usar a conta. A plataforma envia um link de confirmação ao responsável informado; o status fica transparente enquanto a confirmação estiver pendente.",
            "legalBoundary", "A confirmação por e-mail não prova, sozinha, a identidade ou a autoridade legal do responsável. Requisitos adicionais devem ser avaliados antes da publicação em cada país."
        );
    }

    @PostMapping("/me/age-gate")
    @Transactional
    public ResponseEntity<?> ageGate(@Valid @RequestBody AgeGateRequest request) {
        if (request.birthDate() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "A data de nascimento é obrigatória."));
        }
        if (request.birthDate().isAfter(LocalDate.now())) {
            return ResponseEntity.badRequest().body(Map.of("error", "A data de nascimento é inválida."));
        }
        User user = currentUser();
        int age = Period.between(request.birthDate(), LocalDate.now()).getYears();
        if (age < 18) {
            if (isBlank(request.guardianName()) || isBlank(request.guardianEmail()) || isBlank(request.guardianRelationship())) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Para menores de 18 anos, informe nome, e-mail e vínculo do responsável.",
                    "minorAccountSupported", true
                ));
            }
            user.setBirthDate(Date.valueOf(request.birthDate()));
            userRepository.save(user);
            boolean notificationSent = minorGuardianService.request(user, request.guardianName(), request.guardianEmail(), request.guardianRelationship(), request.locale());
            return ResponseEntity.ok(Map.of(
                "accepted", false,
                "minor", true,
                "accessAllowed", true,
                "age", age,
                "guardianStatus", "PENDING_GUARDIAN",
                "guardianNotificationSent", notificationSent,
                "message", notificationSent
                    ? "Conta criada. O responsável recebeu um link de confirmação; o acesso continua disponível enquanto a confirmação estiver pendente."
                    : "Conta criada, mas não foi possível enviar o e-mail ao responsável. O status ficou pendente para reenvio pela equipe."
            ));
        }
        user.setBirthDate(Date.valueOf(request.birthDate()));
        user.setAccountAgeBand("ADULT");
        user.setMinorAccessStatus("NOT_APPLICABLE");
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("accepted", true, "age", age));
    }

    @GetMapping("/guardian-consent")
    public ResponseEntity<?> guardianConsentStatus(@org.springframework.web.bind.annotation.RequestParam String token) {
        try {
            return ResponseEntity.ok(minorGuardianService.publicStatus(token));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @PostMapping("/guardian-consent/confirm")
    @Transactional
    public ResponseEntity<?> confirmGuardianConsent(@Valid @RequestBody GuardianConfirmationRequest request) {
        try {
            return ResponseEntity.ok(minorGuardianService.confirm(request.token(), request.guardianName(), request.relationship(), request.consentGranted()));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
        }
    }

    @GetMapping("/me/minor-status")
    public Map<String, Object> minorStatus() {
        return minorGuardianService.statusFor(currentUser());
    }

    @GetMapping("/me/consents")
    public List<PrivacyConsentDTO> consents() {
        return consentRepository.findByUser_UserIdOrderByRecordedAtDesc(currentUser().getUserId()).stream()
            .map(this::toDTO).toList();
    }

    @PostMapping("/me/consents")
    @Transactional
    public ResponseEntity<?> recordConsent(@Valid @RequestBody ConsentRequest request) {
        if (!PURPOSES.contains(request.purpose())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Finalidade de consentimento inválida."));
        }
        if (("PRIVACY_POLICY".equals(request.purpose()) || "TERMS_OF_SERVICE".equals(request.purpose()))
            && !request.granted()) {
            return ResponseEntity.badRequest().body(Map.of("error", "A política e os termos precisam ser aceitos para criar a conta."));
        }
        PrivacyConsent consent = new PrivacyConsent();
        consent.setUser(currentUser());
        consent.setPurpose(request.purpose());
        consent.setPolicyVersion(blankToDefault(request.policyVersion(), CURRENT_POLICY_VERSION));
        consent.setGranted(request.granted());
        consent.setLocale(blankToDefault(request.locale(), "pt-BR"));
        consent.setSource("web");
        if (!request.granted()) consent.setWithdrawnAt(Instant.now());
        return ResponseEntity.ok(toDTO(consentRepository.save(consent)));
    }

    @PostMapping("/me/consents/{purpose}/revoke")
    @Transactional
    public ResponseEntity<?> revokeConsent(@PathVariable String purpose) {
        if (!PURPOSES.contains(purpose) || !purpose.startsWith("OPTIONAL_")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Somente consentimentos opcionais podem ser revogados por esta rota."));
        }
        PrivacyConsent consent = new PrivacyConsent();
        consent.setUser(currentUser());
        consent.setPurpose(purpose);
        consent.setPolicyVersion(CURRENT_POLICY_VERSION);
        consent.setGranted(false);
        consent.setWithdrawnAt(Instant.now());
        consent.setLocale("pt-BR");
        consent.setSource("privacy-center");
        return ResponseEntity.ok(toDTO(consentRepository.save(consent)));
    }

    @GetMapping("/me/export")
    @Transactional
    public Map<String, Object> exportData() {
        User user = currentUser();
        UUID id = user.getUserId();
        List<ChatMessage> messages = chatRepository.findAllBySenderIdOrReceiverId(id);
        List<Post> posts = postRepository.findByUserUserId(id);
        List<Lesson> lessons = lessonRepository.findByTeacher_UserIdOrStudent_UserIdOrderByScheduledTimeDesc(id, id);
        return Map.ofEntries(
            Map.entry("exportedAt", Instant.now()),
            Map.entry("policyVersion", CURRENT_POLICY_VERSION),
            Map.entry("account", accountData(user)),
            Map.entry("profile", profileData(user.getProfile())),
            Map.entry("skills", user.getSkills().stream().map(skill -> Map.of(
                "id", skill.getSkillId(), "name", skill.getName(), "description", safe(skill.getDescription()),
                "category", safe(skill.getCategory()), "level", safe(skill.getLevel())
            )).toList()),
            Map.entry("consents", consentRepository.findByUser_UserIdOrderByRecordedAtDesc(id).stream().map(this::toDTO).toList()),
            Map.entry("accessLogs", accessLogRepository.findByUserIdOrderByAccessTimeDesc(id)),
            Map.entry("posts", posts.stream().map(post -> Map.of(
                "id", post.getPostId(), "title", safe(post.getTitle()), "content", safe(post.getContent()),
                "createdAt", value(post.getCreatedAt()), "imageUrl", safe(post.getImageUrl()), "videoUrl", safe(post.getVideoUrl())
            )).toList()),
            Map.entry("messages", messages.stream().map(message -> Map.of(
                "id", message.getMessageId(),
                "senderId", value(message.getSender() == null ? null : message.getSender().getUserId()),
                "receiverId", value(message.getReceiver() == null ? null : message.getReceiver().getUserId()),
                "content", safe(message.getContent()), "fileType", safe(message.getFileType()),
                "hasVoice", message.getVoiceData() != null, "hasFile", message.getFileData() != null,
                "sentAt", value(message.getSentAt())
            )).toList()),
            Map.entry("lessons", lessons.stream().map(lesson -> Map.of(
                "id", lesson.getLessonId(), "scheduledTime", value(lesson.getScheduledTime()),
                "status", safe(lesson.getStatus()), "title", safe(lesson.getTitle()),
                "introduction", safe(lesson.getIntroduction()), "durationMinutes", lesson.getDurationMinutes(),
                "completedAt", value(lesson.getCompletedAt()), "certificateCode", safe(lesson.getCertificateCode())
            )).toList())
        );
    }

    @PatchMapping("/me")
    @Transactional
    public ResponseEntity<?> correctPersonalData(@Valid @RequestBody PersonalDataRequest request) {
        User user = currentUser();
        if (request.name() != null) user.setName(request.name().trim());
        if (request.phoneNumber() != null) user.setPhoneNumber(blankToNull(request.phoneNumber()));
        if (request.bio() != null) user.setBio(blankToNull(request.bio()));
        if (request.country() != null) user.setCountry(blankToNull(request.country()));
        if (request.city() != null) user.setCity(blankToNull(request.city()));
        if (request.state() != null) user.setState(blankToNull(request.state()));
        Profile profile = user.getProfile();
        if (profile == null) {
            profile = new Profile();
            profile.setUser(user);
            user.setProfile(profile);
        }
        if (request.description() != null) profile.setDescription(blankToNull(request.description()));
        if (request.location() != null) profile.setLocation(blankToNull(request.location()));
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("updated", true));
    }

    @DeleteMapping("/me")
    @Transactional
    public ResponseEntity<?> deleteOwnAccount() {
        User user = currentUser();
        String suffix = UUID.randomUUID().toString().replace("-", "");
        user.setUsername("deleted_" + suffix.substring(0, 20));
        user.setEmail("deleted-" + suffix + "@invalid.skillswap");
        user.setName("Usuário removido");
        user.setPassword("deleted");
        user.setPhoneNumber(null);
        user.setBirthDate(null);
        user.setBio(null);
        user.setCountry(null);
        user.setCity(null);
        user.setState(null);
        user.setVerificationCode(null);
        user.setVerificationCodeExpiry(null);
        user.setTwoFactorSecret(null);
        user.setFcmToken(null);
        user.setVerified(false);
        user.setVerifiedBadge(false);
        user.setBanned(true);
        user.setDeletedAt(Instant.now());
        user.setAccountAgeBand("ADULT");
        user.setMinorAccessStatus("NOT_APPLICABLE");
        user.setAccessibilityVisualMarker("NEUTRAL_CIRCLE");
        user.setAccessibilityVisualMarkerPublic(false);
        user.setPreferredTheme("default");
        user.setLibrasAvatarEnabled(false);
        user.getRoles().clear();
        user.getSkills().clear();
        user.getFollowingIds().clear();
        user.getCommunityIds().clear();
        user.getMessageIds().clear();
        if (user.getProfile() != null) {
            user.getProfile().setDescription(null);
            user.getProfile().setLocation(null);
            user.getProfile().setContactInfo(null);
            user.getProfile().setSocialMediaLinks(null);
            user.getProfile().setImageUrl(null);
            user.getProfile().setInterests(null);
            user.getProfile().setIsPrivate(true);
            profileRepository.save(user.getProfile());
        }
        guardianConsentRepository.deleteByUser_UserId(user.getUserId());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of(
            "deleted", true,
            "mode", "anonymized",
            "message", "A conta foi desativada e os dados identificáveis foram removidos do perfil. O usuário também deve ser excluído do Firebase pelo cliente."
        ));
    }

    private User currentUser() {
        UserDTO dto = userServiceDTO.getAuthenticatedUser();
        return userRepository.findById(dto.getUserId()).orElseThrow();
    }

    private PrivacyConsentDTO toDTO(PrivacyConsent consent) {
        return new PrivacyConsentDTO(consent.getConsentId(), consent.getPurpose(), consent.getPolicyVersion(),
            consent.isGranted(), consent.getLocale(), consent.getSource(), consent.getRecordedAt(), consent.getWithdrawnAt());
    }

    private Map<String, Object> accountData(User user) {
        return Map.ofEntries(
            Map.entry("userId", user.getUserId()), Map.entry("username", user.getUsername()), Map.entry("name", user.getName()),
            Map.entry("email", user.getEmail()), Map.entry("birthDate", value(user.getBirthDate())), Map.entry("phoneNumber", safe(user.getPhoneNumber())),
            Map.entry("bio", safe(user.getBio())), Map.entry("country", safe(user.getCountry())), Map.entry("city", safe(user.getCity())),
            Map.entry("state", safe(user.getState())), Map.entry("createdAt", value(user.getCreatedAt())), Map.entry("preferredTheme", safe(user.getPreferredTheme())),
            Map.entry("librasAvatarEnabled", user.isLibrasAvatarEnabled()), Map.entry("accessibilityVisualMarker", safe(user.getAccessibilityVisualMarker()))
        );
    }

    private Map<String, Object> profileData(Profile profile) {
        if (profile == null) return Map.of();
        return Map.of(
            "profileId", value(profile.getProfileId()), "description", safe(profile.getDescription()),
            "imageUrl", safe(profile.getImageUrl()), "location", safe(profile.getLocation()),
            "contactInfo", safe(profile.getContactInfo()), "socialMediaLinks", safe(profile.getSocialMediaLinks()),
            "availabilityStatus", safe(profile.getAvailabilityStatus()), "experienceLevel", safe(profile.getExperienceLevel()),
            "educationLevel", safe(profile.getEducationLevel()), "interests", safe(profile.getInterests()),
            "private", Boolean.TRUE.equals(profile.getIsPrivate())
        );
    }

    private static String safe(String value) { return value == null ? "" : value; }
    private static Object value(Object value) { return value == null ? "" : value; }
    private static String blankToDefault(String value, String fallback) { return value == null || value.isBlank() ? fallback : value.trim(); }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private static boolean isBlank(String value) { return value == null || value.isBlank(); }

    public record AgeGateRequest(
        java.time.LocalDate birthDate,
        @Pattern(regexp = "^.{0,120}$", message = "Nome do responsável muito longo") String guardianName,
        @jakarta.validation.constraints.Email(message = "E-mail do responsável inválido")
        @Pattern(regexp = "^.{0,320}$", message = "E-mail do responsável muito longo") String guardianEmail,
        @Pattern(regexp = "^.{0,80}$", message = "Vínculo do responsável muito longo") String guardianRelationship,
        String locale
    ) {}

    public record GuardianConfirmationRequest(
        @NotBlank String token,
        @NotBlank @Pattern(regexp = "^.{1,120}$", message = "Nome do responsável inválido") String guardianName,
        @NotBlank @Pattern(regexp = "^.{1,80}$", message = "Vínculo do responsável inválido") String relationship,
        boolean consentGranted
    ) {}

    public record ConsentRequest(
        @NotBlank String purpose,
        String policyVersion,
        boolean granted,
        String locale
    ) {}

    public record PersonalDataRequest(
        @Pattern(regexp = "^.{0,100}$", message = "Nome muito longo") String name,
        @Pattern(regexp = "^.{0,40}$", message = "Telefone muito longo") String phoneNumber,
        @Pattern(regexp = "^.{0,500}$", message = "Biografia muito longa") String bio,
        @Pattern(regexp = "^.{0,100}$", message = "País muito longo") String country,
        @Pattern(regexp = "^.{0,100}$", message = "Cidade muito longa") String city,
        @Pattern(regexp = "^.{0,100}$", message = "Estado muito longo") String state,
        @Pattern(regexp = "^.{0,5000}$", message = "Descrição muito longa") String description,
        @Pattern(regexp = "^.{0,255}$", message = "Localização muito longa") String location
    ) {}
}
