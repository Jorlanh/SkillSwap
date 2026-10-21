package br.com.teamss.skillswap.skill_swap.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "ss_skill_evidence")
public class SkillEvidence {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "evidence_id")
    private UUID evidenceId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    @JsonIgnore
    private AccessOpportunity opportunity;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(name = "skill_focus", nullable = false, length = 160)
    private String skillFocus;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "artifact_url", length = 1000)
    private String artifactUrl;

    @Column(name = "evidence_type", nullable = false, length = 32)
    private String evidenceType;

    @Column(name = "verification_status", nullable = false, length = 32)
    private String verificationStatus = "SELF_REPORTED";

    @Column(name = "plan_step_id")
    private Long planStepId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getEvidenceId() { return evidenceId; }
    public User getOwner() { return owner; }
    public void setOwner(User value) { owner = value; }
    public AccessOpportunity getOpportunity() { return opportunity; }
    public void setOpportunity(AccessOpportunity value) { opportunity = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getSkillFocus() { return skillFocus; }
    public void setSkillFocus(String value) { skillFocus = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getArtifactUrl() { return artifactUrl; }
    public void setArtifactUrl(String value) { artifactUrl = value; }
    public String getEvidenceType() { return evidenceType; }
    public void setEvidenceType(String value) { evidenceType = value; }
    public String getVerificationStatus() { return verificationStatus; }
    public void setVerificationStatus(String value) { verificationStatus = value; }
    public Long getPlanStepId() { return planStepId; }
    public void setPlanStepId(Long value) { planStepId = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void requestReview() { verificationStatus = "REVIEW_REQUESTED"; updatedAt = Instant.now(); }
}
