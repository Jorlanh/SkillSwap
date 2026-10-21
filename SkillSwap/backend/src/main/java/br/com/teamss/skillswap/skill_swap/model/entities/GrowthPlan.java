package br.com.teamss.skillswap.skill_swap.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "ss_growth_plans")
public class GrowthPlan {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "plan_id")
    private UUID planId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id", nullable = false)
    @JsonIgnore
    private User owner;

    @Column(nullable = false, length = 240)
    private String goal;

    @Column(name = "target_role", length = 160)
    private String targetRole;

    @Column(columnDefinition = "TEXT")
    private String constraints;

    @Column(name = "weekly_hours", nullable = false)
    private int weeklyHours;

    @Column(nullable = false, length = 24)
    private String status = "ACTIVE";

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("stepOrder ASC")
    private List<GrowthPlanStep> steps = new ArrayList<>();

    public UUID getPlanId() { return planId; }
    public User getOwner() { return owner; }
    public void setOwner(User value) { owner = value; }
    public String getGoal() { return goal; }
    public void setGoal(String value) { goal = value; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String value) { targetRole = value; }
    public String getConstraints() { return constraints; }
    public void setConstraints(String value) { constraints = value; }
    public int getWeeklyHours() { return weeklyHours; }
    public void setWeeklyHours(int value) { weeklyHours = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int value) { progressPercent = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void touch() { updatedAt = Instant.now(); }
    public List<GrowthPlanStep> getSteps() { return steps; }
}
