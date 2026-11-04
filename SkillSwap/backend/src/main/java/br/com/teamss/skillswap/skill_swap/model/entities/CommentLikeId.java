package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class CommentLikeId implements Serializable {
    @Column(name = "comment_id")
    private Long commentId;
    @Column(name = "user_id")
    private UUID userId;

    public CommentLikeId() {}
    public CommentLikeId(Long commentId, UUID userId) { this.commentId = commentId; this.userId = userId; }
    public Long getCommentId() { return commentId; }
    public void setCommentId(Long commentId) { this.commentId = commentId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    @Override public boolean equals(Object o) { if (this == o) return true; if (!(o instanceof CommentLikeId that)) return false; return Objects.equals(commentId, that.commentId) && Objects.equals(userId, that.userId); }
    @Override public int hashCode() { return Objects.hash(commentId, userId); }
}
