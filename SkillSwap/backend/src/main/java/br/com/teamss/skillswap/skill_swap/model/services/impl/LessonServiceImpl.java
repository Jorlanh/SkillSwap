package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.LessonScheduleRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.LessonCertificateDTO;
import br.com.teamss.skillswap.skill_swap.dto.LessonViewDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.LessonRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.NotificationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import br.com.teamss.skillswap.skill_swap.model.services.LessonService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import br.com.teamss.skillswap.skill_swap.model.services.PrivacyConsentService;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventAttendee;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Objects;

@Service
public class LessonServiceImpl implements LessonService {

    @Autowired
    private LessonRepository lessonRepository;
    @Autowired
    private EmailService emailService;
    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserServiceDTO userServiceDTO;
    @Autowired
    private PrivacyConsentService privacyConsentService;

    // Injeta o CONTEÚDO do JSON a partir da variável de ambiente
    @Value("${GOOGLE_CREDENTIALS_JSON:}")
    private String credentialsJsonString;

    @Value("${google.calendar.enabled:false}")
    private boolean calendarEnabled;

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "SkillSwap";
    private Calendar calendarService;

    public LessonServiceImpl() {
    }

    @PostConstruct
    public void init() throws IOException, GeneralSecurityException {
        if (!calendarEnabled || credentialsJsonString == null || credentialsJsonString.isBlank()) {
            this.calendarService = null;
            return;
        }

        // Cria um InputStream a partir da String da variável de ambiente
        try (InputStream inputStream = new ByteArrayInputStream(credentialsJsonString.getBytes(StandardCharsets.UTF_8))) {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            GoogleCredentials credentials = GoogleCredentials.fromStream(inputStream)
                    .createScoped(Collections.singletonList("https://www.googleapis.com/auth/calendar"));
            this.calendarService = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
    }

    // ... O resto da sua classe (scheduleLesson, createCalendarEvent, etc.) continua aqui sem alterações ...
    @Override
    public Lesson scheduleLesson(LessonScheduleRequestDTO lessonRequest) {
        User teacher = userRepository.findById(lessonRequest.getTeacherId())
                .orElseThrow(() -> new EntityNotFoundException("Professor não encontrado com ID: " + lessonRequest.getTeacherId()));
        User student = userRepository.findById(lessonRequest.getStudentId())
                .orElseThrow(() -> new EntityNotFoundException("Aluno não encontrado com ID: " + lessonRequest.getStudentId()));

        Lesson lesson = new Lesson();
        lesson.setTeacher(teacher);
        lesson.setStudent(student);
        lesson.setScheduledTime(lessonRequest.getScheduledTime());
        lesson.setStatus("SCHEDULED");
        lesson.setTitle(lessonRequest.getTitle());
        lesson.setIntroduction(lessonRequest.getIntroduction());
        lesson.setDurationMinutes(lessonRequest.getDurationMinutes() > 0 ? lessonRequest.getDurationMinutes() : 60);
        if (privacyConsentService.isGranted(teacher.getUserId(), "OPTIONAL_CALENDAR")
                && privacyConsentService.isGranted(student.getUserId(), "OPTIONAL_CALENDAR")) {
            createCalendarEvent(lesson, teacher.getEmail(), student.getEmail());
        }
        return lessonRepository.save(lesson);
    }

    private void createCalendarEvent(Lesson lesson, String teacherEmail, String studentEmail) {
        if (calendarService == null) {
            return;
        }
        Event event = new Event()
                .setSummary("Aula SkillSwap: " + lesson.getTeacher().getUsername() + " e " + lesson.getStudent().getUsername())
                .setDescription("Aula de troca de habilidades.")
                .setStart(new EventDateTime()
                        .setDateTime(new com.google.api.client.util.DateTime(lesson.getScheduledTime().toEpochMilli()))
                        .setTimeZone("America/Sao_Paulo"))
                .setEnd(new EventDateTime()
                        .setDateTime(new com.google.api.client.util.DateTime(
                                lesson.getScheduledTime().atZone(ZoneId.of("America/Sao_Paulo"))
                                        .plusHours(1).toInstant().toEpochMilli()))
                        .setTimeZone("America/Sao_Paulo"));

        EventAttendee teacher = new EventAttendee().setEmail(teacherEmail);
        EventAttendee student = new EventAttendee().setEmail(studentEmail);
        event.setAttendees(List.of(teacher, student));

        try {
            calendarService.events().insert("primary", event).execute();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao criar evento no Google Calendar: " + e.getMessage());
        }
    }

    @Override
    public List<Lesson> getUpcomingLessons(UUID userId) {
        Instant now = Instant.now();
        return lessonRepository.findByTeacher_UserIdOrStudent_UserIdAndScheduledTimeAfter(userId, userId, now);
    }

    @Override
    public List<LessonViewDTO> getMyLessons() {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return lessonRepository.findByTeacher_UserIdOrStudent_UserIdOrderByScheduledTimeDesc(
                        authenticatedUser.getUserId(), authenticatedUser.getUserId())
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public LessonCertificateDTO completeLesson(Long lessonId) {
        Lesson lesson = findParticipantLesson(lessonId);
        if ("CANCELLED".equalsIgnoreCase(lesson.getStatus())) {
            throw new IllegalStateException("Uma aula cancelada não pode gerar certificado.");
        }

        if (!"COMPLETED".equalsIgnoreCase(lesson.getStatus())) {
            lesson.setStatus("COMPLETED");
            lesson.setCompletedAt(Instant.now());
            if (lesson.getCertificateCode() == null || lesson.getCertificateCode().isBlank()) {
                lesson.setCertificateCode("SS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase());
            }
            lessonRepository.save(lesson);
        }
        return toCertificate(lesson);
    }

    @Override
    public LessonCertificateDTO getCertificate(Long lessonId) {
        Lesson lesson = findParticipantLesson(lessonId);
        if (!"COMPLETED".equalsIgnoreCase(lesson.getStatus())) {
            throw new IllegalStateException("O certificado estará disponível após a conclusão da aula.");
        }
        if (lesson.getCertificateCode() == null || lesson.getCertificateCode().isBlank()) {
            lesson.setCertificateCode("SS-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20).toUpperCase());
            lessonRepository.save(lesson);
        }
        return toCertificate(lesson);
    }

    private Lesson findParticipantLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new EntityNotFoundException("Aula não encontrada."));
        UUID authenticatedId = userServiceDTO.getAuthenticatedUser().getUserId();
        boolean participant = lesson.getTeacher() != null && Objects.equals(lesson.getTeacher().getUserId(), authenticatedId)
                || lesson.getStudent() != null && Objects.equals(lesson.getStudent().getUserId(), authenticatedId);
        if (!participant) {
            throw new org.springframework.security.access.AccessDeniedException("Você não participa desta aula.");
        }
        return lesson;
    }

    private LessonViewDTO toView(Lesson lesson) {
        return new LessonViewDTO(
                lesson.getLessonId(),
                lesson.getTeacher() != null ? lesson.getTeacher().getName() : "Professor",
                lesson.getStudent() != null ? lesson.getStudent().getName() : "Aluno",
                lesson.getScheduledTime(),
                lesson.getStatus(),
                lesson.getTitle(),
                lesson.getIntroduction(),
                lesson.getDurationMinutes(),
                lesson.getCompletedAt(),
                "COMPLETED".equalsIgnoreCase(lesson.getStatus())
        );
    }

    private LessonCertificateDTO toCertificate(Lesson lesson) {
        String title = lesson.getTitle() == null || lesson.getTitle().isBlank()
                ? "Aula de troca de habilidades" : lesson.getTitle();
        String introduction = lesson.getIntroduction() == null || lesson.getIntroduction().isBlank()
                ? "Certificamos a conclusão da aula realizada na plataforma SkillSwap."
                : lesson.getIntroduction();
        return new LessonCertificateDTO(
                lesson.getLessonId(),
                lesson.getCertificateCode(),
                lesson.getStudent() != null ? lesson.getStudent().getName() : "Aluno",
                lesson.getStudent() != null ? lesson.getStudent().getUsername() : "aluno",
                lesson.getTeacher() != null ? lesson.getTeacher().getName() : "Professor",
                title,
                introduction,
                lesson.getDurationMinutes(),
                lesson.getCompletedAt()
        );
    }

    @Override
    public void notifyLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        User teacher = lesson.getTeacher();
        User student = lesson.getStudent();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        ZonedDateTime lessonTime = lesson.getScheduledTime().atZone(ZoneId.of("America/Sao_Paulo"));

        if (now.toLocalDate().equals(lessonTime.toLocalDate())) {
            String lessonDetails = "Aula agendada para hoje às " + lessonTime.toLocalTime() + " com " + teacher.getUsername();
            // ... (lógica de notificação)
        }

        if (now.toLocalTime().isAfter(lessonTime.toLocalTime().minusMinutes(15)) && now.toLocalTime().isBefore(lessonTime.toLocalTime().plusMinutes(15))) {
            String lessonDetails = "A aula com " + teacher.getUsername() + " começa agora às " + lessonTime.toLocalTime();
            // ... (lógica de notificação)
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyNotificationCheck() {
        List<Lesson> lessons = lessonRepository.findAll();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        for (Lesson lesson : lessons) {
            ZonedDateTime lessonTime = lesson.getScheduledTime().atZone(ZoneId.of("America/Sao_Paulo"));
            if (now.toLocalDate().equals(lessonTime.toLocalDate())) {
                notifyLesson(lesson.getLessonId());
            }
        }
    }
}
