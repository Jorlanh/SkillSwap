package br.com.teamss.skillswap.skill_swap.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "ss_growth_plan_steps")
public class GrowthPlanStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "step_id")
    private Long stepId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    @JsonIgnore
    private GrowthPlan plan;

    @Column(name = "step_order", nullable = false)
    private int stepOrder;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 32)
    private String kind;

    @Column(name = "skill_focus", length = 160)
    private String skillFocus;

    @Column(name = "estimated_hours", nullable = false)
    private int estimatedHours;

    @Column(nullable = false, length = 24)
    private String status = "TODO";

    @Column(name = "evidence_id")
    private UUID evidenceId;

    public Long getStepId() { return stepId; }
    public GrowthPlan getPlan() { return plan; }
    public void setPlan(GrowthPlan value) { plan = value; }
    public int getStepOrder() { return stepOrder; }
    public void setStepOrder(int value) { stepOrder = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getKind() { return kind; }
    public void setKind(String value) { kind = value; }
    public String getSkillFocus() { return skillFocus; }
    public void setSkillFocus(String value) { skillFocus = value; }
    public int getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(int value) { estimatedHours = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public UUID getEvidenceId() { return evidenceId; }
    public void setEvidenceId(UUID value) { evidenceId = value; }
}
