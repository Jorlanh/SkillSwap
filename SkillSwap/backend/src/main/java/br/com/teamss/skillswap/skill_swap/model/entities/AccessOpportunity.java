package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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

    @Column(nullable = false, length = 180)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String requiredSkills;

    @Column(nullable = false)
    private boolean remote;

    @Column(nullable = false, length = 20)
    private String participationMode = "REMOTE";

    @Column(nullable = false)
    private boolean accessibleSupport;

    @Column(length = 120)
    private String location;

    @Column(length = 1000)
    private String contactUrl;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    public UUID getOpportunityId() { return opportunityId; }
    public String getOrganizationName() { return organizationName; }
    public void setOrganizationName(String value) { organizationName = value; }
    public String getTitle() { return title; }
    public void setTitle(String value) { title = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public String getRequiredSkills() { return requiredSkills; }
    public void setRequiredSkills(String value) { requiredSkills = value; }
    public boolean isRemote() { return remote; }
    public void setRemote(boolean value) { remote = value; }
    public String getParticipationMode() { return participationMode; }
    public void setParticipationMode(String value) { participationMode = value; remote = "REMOTE".equals(value); }
    public boolean isAccessibleSupport() { return accessibleSupport; }
    public void setAccessibleSupport(boolean value) { accessibleSupport = value; }
    public String getLocation() { return location; }
    public void setLocation(String value) { location = value; }
    public String getContactUrl() { return contactUrl; }
    public void setContactUrl(String value) { contactUrl = value; }
    public Instant getCreatedAt() { return createdAt; }
}
