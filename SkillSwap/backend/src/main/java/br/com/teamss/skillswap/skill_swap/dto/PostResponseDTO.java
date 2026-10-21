package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para exibir um Post de forma segura.
 * Blindado contra valores nulos e objetos complexos.
 */
public class PostResponseDTO {

    private Long postId;
    private String content;
    private String title;
    
    @JsonProperty("user") 
    private UserSummaryDTO user; 

    // Se o seu sistema retorna um objeto para a comunidade, 
    // o Frontend deve acessar community.name
    private Object community; 
    
    private String imageUrl;
    private String videoUrl;
    private Instant createdAt;
    
    // Inicialização garantida para o Algoritmo ELOS e PostCard
    private int likesCount = 0;
    private int repostsCount = 0;
    private int commentsCount = 0;
    private int sharesCount = 0;
    private int viewsCount = 0;

    public PostResponseDTO() {}

    // Getters e Setters
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public UserSummaryDTO getUser() { return user; }
    public void setUser(UserSummaryDTO user) { this.user = user; }
    public Object getCommunity() { return community; }
    public void setCommunity(Object community) { this.community = community; }
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
}