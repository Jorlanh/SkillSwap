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
@Table(name = "ss_organizations")
public class Organization {
    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(nullable = false, length = 180)
    private String name;

    @Column(name = "legal_name", length = 220)
    private String legalName;

    @Column(nullable = false, unique = true, length = 120)
    private String slug;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(length = 80)
    private String country;

    @Column(length = 255)
    private String website;

    @Column(name = "billing_email", length = 255)
    private String billingEmail;

    @Column(nullable = false, length = 24)
    private String status = "ACTIVE";

    @Column(name = "plan_code", nullable = false, length = 32)
    private String planCode = "TRIAL";

    @Column(name = "subscription_status", nullable = false, length = 32)
    private String subscriptionStatus = "TRIAL";

    @Column(name = "seat_limit", nullable = false)
    private int seatLimit = 5;

    @Column(name = "billing_currency", nullable = false, length = 3)
    private String billingCurrency = "USD";

    @Column(name = "monthly_price_cents", nullable = false)
    private long monthlyPriceCents;

    @Column(name = "stripe_customer_id", length = 120)
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id", length = 120)
    private String stripeSubscriptionId;

    @Column(name = "stripe_checkout_session_id", length = 120)
    private String stripeCheckoutSessionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by", nullable = false)
    @JsonIgnore
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getOrganizationId() { return organizationId; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getLegalName() { return legalName; }
    public void setLegalName(String value) { legalName = value; }
    public String getSlug() { return slug; }
    public void setSlug(String value) { slug = value; }
    public String getType() { return type; }
    public void setType(String value) { type = value; }
    public String getCountry() { return country; }
    public void setCountry(String value) { country = value; }
    public String getWebsite() { return website; }
    public void setWebsite(String value) { website = value; }
    public String getBillingEmail() { return billingEmail; }
    public void setBillingEmail(String value) { billingEmail = value; }
    public String getStatus() { return status; }
    public void setStatus(String value) { status = value; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String value) { planCode = value; }
    public String getSubscriptionStatus() { return subscriptionStatus; }
    public void setSubscriptionStatus(String value) { subscriptionStatus = value; }
    public int getSeatLimit() { return seatLimit; }
    public void setSeatLimit(int value) { seatLimit = value; }
    public String getBillingCurrency() { return billingCurrency; }
    public void setBillingCurrency(String value) { billingCurrency = value; }
    public long getMonthlyPriceCents() { return monthlyPriceCents; }
    public void setMonthlyPriceCents(long value) { monthlyPriceCents = value; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public void setStripeCustomerId(String value) { stripeCustomerId = value; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public void setStripeSubscriptionId(String value) { stripeSubscriptionId = value; }
    public String getStripeCheckoutSessionId() { return stripeCheckoutSessionId; }
    public void setStripeCheckoutSessionId(String value) { stripeCheckoutSessionId = value; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User value) { createdBy = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void touch() { updatedAt = Instant.now(); }
}
