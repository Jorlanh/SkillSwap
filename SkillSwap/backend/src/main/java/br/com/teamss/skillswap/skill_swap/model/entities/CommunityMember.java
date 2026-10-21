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
}