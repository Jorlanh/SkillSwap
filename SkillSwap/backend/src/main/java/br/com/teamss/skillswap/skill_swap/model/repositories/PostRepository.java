package br.com.teamss.skillswap.skill_swap.model.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :startTime " +
           "ORDER BY (p.likesCount + p.repostsCount + p.commentsCount + p.sharesCount) DESC")
    List<Post> findTrendingPosts(@Param("startTime") Instant startTime);

    @Query("SELECT p FROM Post p WHERE p.createdAt >= :startTime " +
           "ORDER BY (p.likesCount + p.repostsCount + p.commentsCount + p.sharesCount + p.viewsCount) DESC")
    List<Post> findTrendingPostsWithViews(Instant startTime);

    List<Post> findByUserUserId(UUID userId);

    @Query("SELECT p FROM Post p WHERE p.community.communityId = :communityId AND p.createdAt >= :startTime " +
           "ORDER BY (p.likesCount + p.repostsCount + p.commentsCount + p.sharesCount) DESC")
    List<Post> findByCommunity_CommunityId(@Param("communityId") UUID communityId, @Param("startTime") Instant startTime);

    List<Post> findByCommunity_CommunityId(UUID communityId);

    @Query("SELECT p FROM Post p WHERE p.repostOf IS NOT NULL AND p.user.userId = :userId")
    List<Post> findByRepostOfIsNotNullAndUserUserId(@Param("userId") UUID userId);

    @Query("SELECT p FROM Post p WHERE p.community.communityId IN :communityIds")
    List<Post> findByCommunity_CommunityIdIn(@Param("communityIds") List<UUID> communityIds);
}