package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.math.BigDecimal;

@Entity
@Table(name = "tb_reports")
@Getter
@Setter
@NoArgsConstructor
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_user_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private User reportedUser;

    @Column(length = 1000)
    private String reason;

    @Column(name = "reported_content", columnDefinition = "TEXT")
    private String reportedContent;

    @Column(name = "source", length = 32, nullable = false)
    private String source = "USER_REPORT";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportStatus status = ReportStatus.PENDING;

    @CreationTimestamp
    private Instant createdAt;

    /** Triagem da IA. Esses campos não são uma decisão de banimento. */
    @Column(name = "ai_category", length = 64)
    private String aiCategory;

    @Column(name = "ai_confidence", precision = 5, scale = 4)
    private BigDecimal aiConfidence;

    @Column(name = "ai_recommendation", length = 32)
    private String aiRecommendation;

    @Column(name = "contained_until")
    private Instant containedUntil;

    /** Registro da decisão humana e dos fundamentos usados. */
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_action", length = 32)
    private ModerationAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "decision_reason", length = 2000)
    private String decisionReason;

    @Column(name = "decision_evidence", columnDefinition = "TEXT")
    private String decisionEvidence;

    @Column(name = "appeal_status", length = 32, nullable = false)
    private String appealStatus = "NOT_REQUESTED";

    @Column(name = "appeal_reason", length = 2000)
    private String appealReason;

    @Column(name = "appealed_at")
    private Instant appealedAt;

    @Column(name = "automatic_temporary_ban_applied", nullable = false)
    private boolean automaticTemporaryBanApplied = false;

    @Column(name = "automatic_action_at")
    private Instant automaticActionAt;

    @Column(name = "automatic_action_reason", length = 1000)
    private String automaticActionReason;
}
