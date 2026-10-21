package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects; // ADICIONADO
import java.util.UUID;

@Embeddable
public class CommunityMemberId implements Serializable {
    @Column(name = "community_id")
    private UUID communityId;

    @Column(name = "user_id")
    private UUID userId;

    // Getters e Setters
    public UUID getCommunityId() {
        return communityId;
    }

    public void setCommunityId(UUID communityId) {
        this.communityId = communityId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    // Equals e HashCode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CommunityMemberId that = (CommunityMemberId) o;
        return Objects.equals(communityId, that.communityId) && Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(communityId, userId);
    }
}