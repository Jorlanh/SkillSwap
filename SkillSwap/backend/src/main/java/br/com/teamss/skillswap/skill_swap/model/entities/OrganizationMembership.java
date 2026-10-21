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
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

@Entity
@Table(name = "ss_organization_memberships", uniqueConstraints = @UniqueConstraint(name = "uk_org_membership_user", columnNames = {"organization_id", "user_id"}))
public class OrganizationMembership {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "membership_id")
    private UUID membershipId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    @JsonIgnore
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @Column(nullable = false, length = 24)
    private String role = "MEMBER";

    @Column(nullable = false, length = 24)
    private String status = "ACTIVE";

    @Column(length = 120)
    private String department;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getMembershipId() { return membershipId; }
    public Organization getOrganization() { return organization; }
    public void setOrganization(Organization value) { organization = value; }
    public User getUser() { return user; }
    public void setUser(User value) { user = value; }
    public String getRole() { return role; }
    public void setRole(String value) { role = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getDepartment() { return department; }
    public void setDepartment(String value) { department = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void touch() { updatedAt = Instant.now(); }
}
