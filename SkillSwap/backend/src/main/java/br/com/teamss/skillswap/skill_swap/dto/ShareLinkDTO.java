package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;
import java.util.UUID;

public class ShareLinkDTO {
    private Long shareId;
    private Long postId;
    private UUID userId;
    private String shareUrl;
    private Instant createdAt;
    private int clickCount;

    // Getters e Setters
    public Long getShareId() { return shareId; }
    public void setShareId(Long shareId) { this.shareId = shareId; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getShareUrl() { return shareUrl; }
    public void setShareUrl(String shareUrl) { this.shareUrl = shareUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public int getClickCount() { return clickCount; }
    public void setClickCount(int clickCount) { this.clickCount = clickCount; }
}