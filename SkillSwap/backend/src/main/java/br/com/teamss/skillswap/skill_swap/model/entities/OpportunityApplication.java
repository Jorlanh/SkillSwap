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
@Table(name = "ss_opportunity_applications")
public class OpportunityApplication {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "application_id")
    private UUID applicationId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "applicant_id", nullable = false)
    @JsonIgnore
    private User applicant;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "opportunity_id", nullable = false)
    private AccessOpportunity opportunity;

    @Column(nullable = false, length = 24)
    private String status = "SUBMITTED";

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getApplicationId() { return applicationId; }
    public User getApplicant() { return applicant; }
    public void setApplicant(User value) { applicant = value; }
    public AccessOpportunity getOpportunity() { return opportunity; }
    public void setOpportunity(AccessOpportunity value) { opportunity = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; updatedAt = Instant.now(); }
    public String getMessage() { return message; }
    public void setMessage(String value) { message = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
