package br.com.teamss.skillswap.skill_swap.model.premium;

import br.com.teamss.skillswap.skill_swap.model.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;

/** Monthly quota bucket. A row is created lazily on the first premium call. */
@Entity
@Table(name = "premium_ai_usage", uniqueConstraints = @UniqueConstraint(
    name = "uk_premium_ai_usage_user_period", columnNames = {"user_id", "period_start"}))
public class PremiumUsage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "usage_id")
    private java.util.UUID usageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "free_ai_prompts_used", nullable = false)
    private int freeAiPromptsUsed;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public java.util.UUID getUsageId() { return usageId; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public LocalDate getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDate periodStart) { this.periodStart = periodStart; }
    public int getFreeAiPromptsUsed() { return freeAiPromptsUsed; }
    public void setFreeAiPromptsUsed(int freeAiPromptsUsed) { this.freeAiPromptsUsed = freeAiPromptsUsed; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void touch() { this.updatedAt = Instant.now(); }
}
