package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.CommunitySummaryDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Community;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.Like;
import br.com.teamss.skillswap.skill_swap.model.entities.Comment;
import br.com.teamss.skillswap.skill_swap.model.entities.Repost;
import br.com.teamss.skillswap.skill_swap.model.entities.ShareLink;
import br.com.teamss.skillswap.skill_swap.dto.CommentDTO;
import br.com.teamss.skillswap.skill_swap.dto.CreateCommunityRequest;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import br.com.teamss.skillswap.skill_swap.dto.CommunityMemberDTO;

public interface CommunityService {
    List<CommunitySummaryDTO> getCommunities(int limit);
    CommunitySummaryDTO createCommunity(CreateCommunityRequest request, UUID creatorId);
    CommunitySummaryDTO getCommunity(UUID communityId);
    void updateCommunityImage(UUID communityId, UUID userId, MultipartFile image) throws IOException;
    Community joinCommunity(UUID communityId, UUID userId);
    List<Post> getCommunityPosts(UUID communityId);
    Post createCommunityPost(UUID communityId, UUID userId, String title, String content, 
                             MultipartFile image, MultipartFile video) throws IOException;
    Like likePost(UUID communityId, Long postId, UUID userId);
    Comment commentPost(UUID communityId, Long postId, CommentDTO commentDTO);
    Repost repostPost(UUID communityId, Long postId, UUID userId);
    ShareLink sharePost(UUID communityId, Long postId, UUID userId);
    List<CommunityMemberDTO> listMembers(UUID communityId, UUID actorId, String search);
    List<CommunityMemberDTO> listPendingMembers(UUID communityId, UUID actorId, String search);
    String getMembershipStatus(UUID communityId, UUID userId);
    void removeMember(UUID communityId, UUID actorId, UUID memberId);
    void setAdministrator(UUID communityId, UUID actorId, UUID memberId, boolean administrator);
    void approveMember(UUID communityId, UUID actorId, UUID memberId);
}
