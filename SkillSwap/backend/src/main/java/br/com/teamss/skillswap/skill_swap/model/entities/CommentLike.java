package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tb_comment_likes")
public class CommentLike {
    @EmbeddedId
    private CommentLikeId id;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("commentId")
    @JoinColumn(name = "comment_id")
    private Comment comment;
    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
    public CommentLike() {}
    public CommentLike(Comment comment, User user) { this.comment = comment; this.user = user; this.id = new CommentLikeId(comment.getCommentId(), user.getUserId()); }
    public CommentLikeId getId() { return id; }
    public void setId(CommentLikeId id) { this.id = id; }
    public Comment getComment() { return comment; }
    public void setComment(Comment comment) { this.comment = comment; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
