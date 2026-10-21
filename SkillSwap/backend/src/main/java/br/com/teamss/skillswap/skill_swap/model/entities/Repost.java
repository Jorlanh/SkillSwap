package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;

@Entity
@Table(name = "tb_reposts")
public class Repost {

    @EmbeddedId
    private RepostId id;

    @ManyToOne
    @MapsId("postId")
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne
    @MapsId("userId")
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "created_at")
    private Instant  createdAt;

    // Construtores
    public Repost() {}

    public Repost(Post post, User user) {
        this.id = new RepostId(post.getPostId(), user.getUserId());
        this.post = post;
        this.user = user;
        this.createdAt = Instant.now();
    }

    // Getters e Setters
    public RepostId getId() { return id; }
    public void setId(RepostId id) { this.id = id; }
    public Post getPost() { return post; }
    public void setPost(Post post) { this.post = post; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Instant  getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant  createdAt) { this.createdAt = createdAt; }
}