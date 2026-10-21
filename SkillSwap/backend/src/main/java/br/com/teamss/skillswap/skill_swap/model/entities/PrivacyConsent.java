package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ss_privacy_consents")
public class PrivacyConsent {
    @Id
    @Column(name = "consent_id", nullable = false)
    private UUID consentId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "purpose", nullable = false, length = 80)
    private String purpose;

    @Column(name = "policy_version", nullable = false, length = 32)
    private String policyVersion;

    @Column(name = "granted", nullable = false)
    private boolean granted;

    @Column(name = "locale", nullable = false, length = 16)
    private String locale = "pt-BR";

    @Column(name = "source", nullable = false, length = 32)
    private String source = "web";

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @PrePersist
    void beforeInsert() {
        if (consentId == null) consentId = UUID.randomUUID();
        if (recordedAt == null) recordedAt = Instant.now();
    }

    public UUID getConsentId() { return consentId; }
    public void setConsentId(UUID consentId) { this.consentId = consentId; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(String policyVersion) { this.policyVersion = policyVersion; }
    public boolean isGranted() { return granted; }
    public void setGranted(boolean granted) { this.granted = granted; }
    public String getLocale() { return locale; }
    public void setLocale(String locale) { this.locale = locale; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getRecordedAt() { return recordedAt; }
    public void setRecordedAt(Instant recordedAt) { this.recordedAt = recordedAt; }
    public Instant getWithdrawnAt() { return withdrawnAt; }
    public void setWithdrawnAt(Instant withdrawnAt) { this.withdrawnAt = withdrawnAt; }
}
