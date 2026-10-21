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
@Table(name = "ss_guardian_consents")
public class GuardianConsent {
    @Id
    @Column(name = "guardian_consent_id", nullable = false)
    private UUID guardianConsentId;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "guardian_name", nullable = false, length = 120)
    private String guardianName;

    @Column(name = "guardian_email", nullable = false, length = 320)
    private String guardianEmail;

    @Column(name = "relationship", nullable = false, length = 80)
    private String relationship;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "status", nullable = false, length = 24)
    private String status = "PENDING";

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "policy_version", nullable = false, length = 32)
    private String policyVersion;

    @Column(name = "locale", nullable = false, length = 16)
    private String locale = "pt-BR";

    @PrePersist
    void beforeInsert() {
        if (guardianConsentId == null) guardianConsentId = UUID.randomUUID();
        if (requestedAt == null) requestedAt = Instant.now();
    }

    public UUID getGuardianConsentId() { return guardianConsentId; }
    public void setGuardianConsentId(UUID value) { this.guardianConsentId = value; }
    public User getUser() { return user; }
    public void setUser(User value) { this.user = value; }
    public String getGuardianName() { return guardianName; }
    public void setGuardianName(String value) { this.guardianName = value; }
    public String getGuardianEmail() { return guardianEmail; }
    public void setGuardianEmail(String value) { this.guardianEmail = value; }
    public String getRelationship() { return relationship; }
    public void setRelationship(String value) { this.relationship = value; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String value) { this.tokenHash = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { this.status = value; }
    public Instant getRequestedAt() { return requestedAt; }
    public void setRequestedAt(Instant value) { this.requestedAt = value; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant value) { this.expiresAt = value; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(Instant value) { this.confirmedAt = value; }
    public String getPolicyVersion() { return policyVersion; }
    public void setPolicyVersion(String value) { this.policyVersion = value; }
    public String getLocale() { return locale; }
    public void setLocale(String value) { this.locale = value; }
}
