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
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "lesson_id")
    private Long lessonId;

    @ManyToOne
    @JoinColumn(name = "teacher_id")
    private User teacher;

    @ManyToOne
    @JoinColumn(name = "student_id")
    private User student;

    @Column(name = "scheduled_time")
    private Instant scheduledTime;

    @Column(name = "status")
    private String status;

    @Column(name = "title", length = 200)
    private String title;

    @Column(name = "introduction", columnDefinition = "TEXT")
    private String introduction;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 60;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "certificate_code", length = 80, unique = true)
    private String certificateCode;
}
