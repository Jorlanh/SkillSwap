package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "tb_access_opportunities")
public class AccessOpportunity {
    @Id
    @GeneratedValue
    @UuidGenerator
    private UUID opportunityId;

    @Column(nullable = false, length = 160)
    private String organizationName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private User createdBy;

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(nullable = false)
    private boolean remote;

    @Column(nullable = false)
    private boolean accessibleSupport;

    @Column(length = 120)
    private String location;

    @Column(nullable = false, length = 24)
    private String status = "OPEN";

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getOpportunityId() { return opportunityId; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String value) { organizationName = value; }
    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization value) { organization = value; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User value) { createdBy = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String value) { requiredSkills = value; }
    public boolean isRemote() { return remote; }
    public void setRemote(boolean value) { remote = value; }
    public boolean isAccessibleSupport() { return accessibleSupport; }
    public void setAccessibleSupport(boolean value) { accessibleSupport = value; }
    public String getLocation() { return location; }
    public void setLocation(String value) { location = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public Instant getCreatedAt() { return createdAt; }
}
