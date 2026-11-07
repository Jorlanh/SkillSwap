package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "tb_lesson_reviews",
       uniqueConstraints = @UniqueConstraint(name = "ux_lesson_review_reviewer", columnNames = {"lesson_id", "reviewer_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reviewed_user_id", nullable = false)
    private User reviewedUser;

    @Column(nullable = false)
    private Integer stars;

    @Column(length = 1200)
    private String comment;

    @Column(name = "skill_subject", nullable = false, length = 180)
    private String skillSubject;

    @Column(name = "reviewer_full_name", nullable = false, length = 180)
    private String reviewerFullName;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
