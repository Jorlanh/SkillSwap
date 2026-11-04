package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_community_members")
public class CommunityMember {
    @EmbeddedId
    private CommunityMemberId id;

    @Column(name = "joined_at")
    private Instant joinedAt;

    @Column(name = "member_role", nullable = false, length = 20)
    private String memberRole = "MEMBER";

    @Column(name = "membership_status", nullable = false, length = 20)
    private String membershipStatus = "APPROVED";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    // Getters e Setters
    public CommunityMemberId getId() {
        return id;
    }

    public void setId(CommunityMemberId id) {
        this.id = id;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }
    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) { this.memberRole = memberRole; }
    public String getMembershipStatus() { return membershipStatus; }
    public void setMembershipStatus(String membershipStatus) { this.membershipStatus = membershipStatus; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
}
