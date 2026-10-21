package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "ss_moderation_settings")
public class ModerationSettings {
    @Id
    private Integer id = 1;

    @Column(name = "automatic_temporary_ban_enabled", nullable = false)
    private boolean automaticTemporaryBanEnabled = false;

    @Column(name = "automatic_temporary_ban_hours", nullable = false)
    private int automaticTemporaryBanHours = 24;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public boolean isAutomaticTemporaryBanEnabled() { return automaticTemporaryBanEnabled; }
    public void setAutomaticTemporaryBanEnabled(boolean enabled) { this.automaticTemporaryBanEnabled = enabled; }
    public int getAutomaticTemporaryBanHours() { return automaticTemporaryBanHours; }
    public void setAutomaticTemporaryBanHours(int hours) { this.automaticTemporaryBanHours = hours; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
