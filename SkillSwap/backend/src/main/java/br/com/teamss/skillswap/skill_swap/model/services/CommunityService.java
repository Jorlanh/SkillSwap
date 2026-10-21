package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.model.entities.Community;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.Like;
import br.com.teamss.skillswap.skill_swap.model.entities.Comment;
import br.com.teamss.skillswap.skill_swap.model.entities.Repost;
import br.com.teamss.skillswap.skill_swap.model.entities.ShareLink;
import br.com.teamss.skillswap.skill_swap.dto.CommentDTO;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface CommunityService {
    List<Community> getCommunities(int limit);
    Community joinCommunity(UUID communityId, UUID userId);
    List<Post> getCommunityPosts(UUID communityId);
    Post createCommunityPost(UUID communityId, UUID userId, String title, String content, 
                             MultipartFile image, MultipartFile video) throws IOException;
    Like likePost(UUID communityId, Long postId, UUID userId);
    Comment commentPost(UUID communityId, Long postId, CommentDTO commentDTO);
    Repost repostPost(UUID communityId, Long postId, UUID userId);
    ShareLink sharePost(UUID communityId, Long postId, UUID userId);
}