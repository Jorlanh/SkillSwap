package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_posts")
public class Post {

    @Id
    @Column(name = "post_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId;

    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "title", nullable = false)
    private String title;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "profile_id")
    private Profile profile;

    @ManyToOne
    @JoinColumn(name = "community_id")
    private Community community;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "video_url")
    private String videoUrl;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "likes_count", nullable = false)
    private int likesCount;

    @Column(name = "reposts_count", nullable = false)
    private int repostsCount;

    @Column(name = "comments_count", nullable = false)
    private int commentsCount;

    @Column(name = "shares_count", nullable = false)
    private int sharesCount;

    @Column(name = "views_count", nullable = false)
    private int viewsCount;

    @Column(name = "repost_of")
    private Long repostOf;

    @Column(name = "share_url")
    private String shareUrl;

    @Column(name = "highlighted_until")
    private Instant highlightedUntil;

    @Transient
    private double trendingScore;

    public Post() {}

    public Post(Long postId, String content, String title, User user, Profile profile, Community community,
                String imageUrl, String videoUrl, Instant createdAt, int likesCount, int repostsCount,
                int commentsCount, int sharesCount, int viewsCount) {
        this.postId = postId;
        this.content = content;
        this.title = title;
        this.user = user;
        this.profile = profile;
        this.community = community;
        this.imageUrl = imageUrl;
        this.videoUrl = videoUrl;
        this.createdAt = createdAt;
        this.likesCount = likesCount;
        this.repostsCount = repostsCount;
        this.commentsCount = commentsCount;
        this.sharesCount = sharesCount;
        this.viewsCount = viewsCount;
    }

    // Getters e Setters
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Profile getProfile() { return profile; }
    public void setProfile(Profile profile) { this.profile = profile; }
    public Community getCommunity() { return community; }
    public void setCommunity(Community community) { this.community = community; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
    public int getRepostsCount() { return repostsCount; }
    public void setRepostsCount(int repostsCount) { this.repostsCount = repostsCount; }
    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }
    public int getSharesCount() { return sharesCount; }
    public void setSharesCount(int sharesCount) { this.sharesCount = sharesCount; }
    public int getViewsCount() { return viewsCount; }
    public void setViewsCount(int viewsCount) { this.viewsCount = viewsCount; }
    public Long getRepostOf() { return repostOf; }
    public void setRepostOf(Long repostOf) { this.repostOf = repostOf; }
    public String getShareUrl() { return shareUrl; }
    public void setShareUrl(String shareUrl) { this.shareUrl = shareUrl; }
    public double getTrendingScore() { return trendingScore; }
    public void setTrendingScore(double trendingScore) { this.trendingScore = trendingScore; }
    public Instant getHighlightedUntil() { return highlightedUntil; }
    public void setHighlightedUntil(Instant highlightedUntil) { this.highlightedUntil = highlightedUntil; }
}
