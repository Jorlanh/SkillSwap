package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.LessonScheduleRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.LessonCompletionResponse;
import br.com.teamss.skillswap.skill_swap.dto.LessonCertificateResponse;
import br.com.teamss.skillswap.skill_swap.dto.LessonReviewRequest;
import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import br.com.teamss.skillswap.skill_swap.model.entities.LessonReview;
import br.com.teamss.skillswap.skill_swap.model.entities.Notification;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.LessonRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.LessonReviewRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.NotificationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import br.com.teamss.skillswap.skill_swap.model.services.LessonService;
import br.com.teamss.skillswap.skill_swap.model.services.NotificationService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
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
import org.springframework.transaction.annotation.Transactional;
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
    private LessonReviewRepository lessonReviewRepository;
    @Autowired
    private NotificationService notificationService;

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
        if (!calendarEnabled) {
            return;
        }
        if (credentialsJsonString == null || credentialsJsonString.isBlank()) {
            throw new IllegalStateException("A variável de ambiente 'GOOGLE_CREDENTIALS_JSON' não foi definida.");
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
    @Transactional
    public Lesson scheduleLesson(LessonScheduleRequestDTO lessonRequest) {
        User teacher = userRepository.findById(lessonRequest.getTeacherId())
                .orElseThrow(() -> new EntityNotFoundException("Professor não encontrado com ID: " + lessonRequest.getTeacherId()));
        User student = userRepository.findById(lessonRequest.getStudentId())
                .orElseThrow(() -> new EntityNotFoundException("Aluno não encontrado com ID: " + lessonRequest.getStudentId()));

        Lesson lesson = new Lesson();
        lesson.setTeacher(teacher);
        lesson.setStudent(student);
        String requestedTitle = lessonRequest.getTitle();
        lesson.setTitle(requestedTitle == null || requestedTitle.isBlank() ? "Aula SkillSwap" : requestedTitle.trim());
        lesson.setDurationMinutes(60);
        lesson.setScheduledTime(lessonRequest.getScheduledTime());
        lesson.setStatus("SCHEDULED");
        Lesson savedLesson = lessonRepository.save(lesson);
        try {
            createCalendarEvent(savedLesson, teacher.getEmail(), student.getEmail());
        } catch (RuntimeException exception) {
            // O registro da aula continua válido mesmo quando a integração
            // opcional do Google Calendar estiver indisponível.
            System.err.println("Não foi possível criar o evento no Google Calendar: " + exception.getMessage());
        }
        notifyParticipants(savedLesson,
                "Aula \"" + savedLesson.getTitle() + "\" agendada para " + savedLesson.getScheduledTime()
                        + " com " + teacher.getUsername() + ".");
        return savedLesson;
    }

    private void createCalendarEvent(Lesson lesson, String teacherEmail, String studentEmail) {
        if (!calendarEnabled) {
            return;
        }
        if (calendarService == null) {
            throw new IllegalStateException("O Google Calendar está habilitado, mas não foi inicializado.");
        }
        Event event = new Event()
                .setSummary("SkillSwap: " + lesson.getTitle())
                .setDescription("Aula de troca de habilidades entre "
                        + lesson.getTeacher().getUsername() + " e " + lesson.getStudent().getUsername() + ".")
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
    public List<Lesson> getLessonsForUser(UUID userId) {
        return lessonRepository.findByTeacher_UserIdOrStudent_UserIdOrderByScheduledTimeDesc(userId, userId);
    }

    @Override
    @Transactional
    public LessonCompletionResponse confirmCompletion(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new EntityNotFoundException("Aula não encontrada."));
        UUID currentUserId = userServiceDTO.getAuthenticatedUser().getUserId();
        if (lesson.getTeacher() == null || lesson.getStudent() == null
            || (!currentUserId.equals(lesson.getTeacher().getUserId())
                && !currentUserId.equals(lesson.getStudent().getUserId()))) {
            throw new SecurityException("Somente participantes podem confirmar a aula.");
        }
        if (currentUserId.equals(lesson.getTeacher().getUserId())) {
            lesson.setTeacherConfirmed(true);
        }
        if (currentUserId.equals(lesson.getStudent().getUserId())) {
            lesson.setStudentConfirmed(true);
        }
        if (lesson.isTeacherConfirmed() && lesson.isStudentConfirmed()) {
            lesson.setStatus("COMPLETED");
            lesson.setCompletedAt(Instant.now());
        }
        lessonRepository.save(lesson);
        boolean confirmed = currentUserId.equals(lesson.getTeacher().getUserId())
            ? lesson.isTeacherConfirmed() : lesson.isStudentConfirmed();
        return new LessonCompletionResponse(lesson.getLessonId(), lesson.getStatus(), confirmed,
            "COMPLETED".equals(lesson.getStatus()));
    }

    @Override
    @Transactional
    public void submitReview(Long lessonId, LessonReviewRequest request) {
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new EntityNotFoundException("Aula não encontrada."));
        User current = userRepository.findById(userServiceDTO.getAuthenticatedUser().getUserId())
            .orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
        boolean isTeacher = lesson.getTeacher() != null && current.getUserId().equals(lesson.getTeacher().getUserId());
        boolean isStudent = lesson.getStudent() != null && current.getUserId().equals(lesson.getStudent().getUserId());
        if (!isTeacher && !isStudent) {
            throw new SecurityException("Somente participantes podem avaliar esta aula.");
        }
        if (lessonReviewRepository.existsByLessonLessonIdAndReviewerUserId(lessonId, current.getUserId())) {
            throw new IllegalStateException("Você já avaliou esta aula.");
        }
        User reviewed = isTeacher ? lesson.getStudent() : lesson.getTeacher();
        LessonReview review = new LessonReview();
        review.setLesson(lesson);
        review.setReviewer(current);
        review.setReviewedUser(reviewed);
        review.setStars(request.stars());
        review.setComment(request.comment() == null ? "" : request.comment().trim());
        review.setSkillSubject(request.skillSubject().trim());
        review.setReviewerFullName(request.fullName().trim());
        lessonReviewRepository.save(review);

        if (isTeacher) {
            lesson.setTeacherCertificateName(request.fullName().trim());
        } else {
            lesson.setStudentCertificateName(request.fullName().trim());
        }
        lessonRepository.save(lesson);
        String comment = request.comment() == null || request.comment().isBlank()
            ? "Sem comentário."
            : request.comment().trim();
        notificationService.createNotification(reviewed.getUserId(),
            "@" + current.getUsername() + " avaliou a aula \"" + lesson.getTitle()
                + "\" com " + request.stars() + " estrelas. Comentário: " + comment);
    }

    @Override
    @Transactional(readOnly = true)
    public LessonCertificateResponse getCertificate(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
            .orElseThrow(() -> new EntityNotFoundException("Aula não encontrada."));
        UUID currentUserId = userServiceDTO.getAuthenticatedUser().getUserId();
        boolean participant = (lesson.getStudent() != null && currentUserId.equals(lesson.getStudent().getUserId()))
            || (lesson.getTeacher() != null && currentUserId.equals(lesson.getTeacher().getUserId()));
        if (!participant) {
            throw new SecurityException("O certificado está disponível apenas para os participantes.");
        }
        if (!"COMPLETED".equals(lesson.getStatus()) || lesson.getCompletedAt() == null) {
            throw new IllegalStateException("O certificado será liberado após a confirmação dos dois participantes.");
        }
        String certificateId = "SKILLSWAP-" + lesson.getLessonId() + "-" + lesson.getCompletedAt().toEpochMilli();
        String studentName = lesson.getStudentCertificateName() == null || lesson.getStudentCertificateName().isBlank()
            ? lesson.getStudent().getName() : lesson.getStudentCertificateName();
        String teacherName = lesson.getTeacherCertificateName() == null || lesson.getTeacherCertificateName().isBlank()
            ? lesson.getTeacher().getName() : lesson.getTeacherCertificateName();
        return new LessonCertificateResponse(certificateId, studentName,
            teacherName, lesson.getTitle(), lesson.getDurationMinutes(), lesson.getCompletedAt());
    }
    @Override
    public List<Lesson> getUpcomingLessons(UUID userId) {
        Instant now = Instant.now();
        return lessonRepository.findByTeacher_UserIdOrStudent_UserIdAndScheduledTimeAfter(userId, userId, now);
    }

    @Override
    @Transactional
    public void notifyLesson(Long lessonId) {
        Lesson lesson = lessonRepository.findById(lessonId)
                .orElseThrow(() -> new RuntimeException("Lesson not found"));
        if (lesson.getScheduledTime() == null) {
            return;
        }
        User teacher = lesson.getTeacher();
        User student = lesson.getStudent();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        ZonedDateTime lessonTime = lesson.getScheduledTime().atZone(ZoneId.of("America/Sao_Paulo"));

        String lessonDetails = null;
        if (now.toLocalTime().isAfter(lessonTime.toLocalTime().minusMinutes(15))
                && now.toLocalTime().isBefore(lessonTime.toLocalTime().plusMinutes(15))) {
            lessonDetails = "A aula \"" + lesson.getTitle() + "\" com " + teacher.getUsername()
                    + " começa agora às " + lessonTime.toLocalTime() + ".";
            Instant lastReminder = lesson.getLastReminderSentAt();
            Instant reminderWindowStart = lesson.getScheduledTime().minusSeconds(15 * 60L);
            if (lastReminder != null && !lastReminder.isBefore(reminderWindowStart)) {
                return;
            }
            lesson.setLastReminderSentAt(Instant.now());
            lessonRepository.save(lesson);
        } else if (now.toLocalDate().equals(lessonTime.toLocalDate())) {
            lessonDetails = "Aula \"" + lesson.getTitle() + "\" agendada para hoje às " + lessonTime.toLocalTime()
                    + " com " + teacher.getUsername() + ".";
        }
        if (lessonDetails != null) {
            notifyParticipants(lesson, lessonDetails);
        }
    }

    private void notifyParticipants(Lesson lesson, String message) {
        for (User participant : List.of(lesson.getTeacher(), lesson.getStudent())) {
            Notification notification = new Notification();
            notification.setUser(participant);
            notification.setMessage(message);
            notification.setSentAt(Instant.now());
            notification.setRead(false);
            notificationRepository.save(notification);
            try {
                emailService.sendLessonNotification(participant.getEmail(), message);
            } catch (RuntimeException exception) {
                System.err.println("Aviso: não foi possível enviar o e-mail da aula: " + exception.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyNotificationCheck() {
        List<Lesson> lessons = lessonRepository.findAll();
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
        for (Lesson lesson : lessons) {
            if (lesson.getScheduledTime() == null) {
                continue;
            }
            ZonedDateTime lessonTime = lesson.getScheduledTime().atZone(ZoneId.of("America/Sao_Paulo"));
            if (now.toLocalDate().equals(lessonTime.toLocalDate())) {
                notifyLesson(lesson.getLessonId());
            }
        }
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void lessonTimeNotificationCheck() {
        Instant now = Instant.now();
        // A consulta ampla permite verificar as aulas de todos os participantes.
        for (Lesson lesson : lessonRepository.findAll()) {
            if (lesson.getScheduledTime() == null) {
                continue;
            }
            Instant scheduled = lesson.getScheduledTime();
            if (!scheduled.isBefore(now.minusSeconds(15 * 60L))
                    && !scheduled.isAfter(now.plusSeconds(15 * 60L))) {
                notifyLesson(lesson.getLessonId());
            }
        }
    }
}
