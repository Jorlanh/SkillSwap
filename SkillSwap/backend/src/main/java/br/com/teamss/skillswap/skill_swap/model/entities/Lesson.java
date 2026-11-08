package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "lesson_id")
    private Long lessonId;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "title", nullable = false, length = 180)
    private String title = "Aula SkillSwap";

    @Column(name = "scheduled_time")
    private Instant scheduledTime;

    @Column(name = "duration_minutes", nullable = false)
    private Integer durationMinutes = 60;

    @Column(name = "status")
    private String status;

    @Column(name = "teacher_confirmed", nullable = false)
    private boolean teacherConfirmed;

    @Column(name = "student_confirmed", nullable = false)
    private boolean studentConfirmed;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "last_reminder_sent_at")
    private Instant lastReminderSentAt;

    @Column(name = "midnight_reminder_sent_at")
    private Instant midnightReminderSentAt;

    @Column(name = "one_hour_reminder_sent_at")
    private Instant oneHourReminderSentAt;

    @Column(name = "start_reminder_sent_at")
    private Instant startReminderSentAt;

    @Column(name = "teacher_certificate_name", length = 180)
    private String teacherCertificateName;

    @Column(name = "student_certificate_name", length = 180)
    private String studentCertificateName;
}
