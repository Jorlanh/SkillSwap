package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.CommentDTO;
import br.com.teamss.skillswap.skill_swap.dto.CommunitySummaryDTO;
import br.com.teamss.skillswap.skill_swap.dto.CreateCommunityRequest;
import br.com.teamss.skillswap.skill_swap.model.entities.*;
import br.com.teamss.skillswap.skill_swap.model.exception.InappropriateContentException;
import br.com.teamss.skillswap.skill_swap.model.repositories.*;
import br.com.teamss.skillswap.skill_swap.model.services.CommunityService;
import br.com.teamss.skillswap.skill_swap.model.services.ContentModerationService;
import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommunityServiceImpl implements CommunityService {

    private final CommunityRepository communityRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final ProfileRepository profileRepository;
    private final LikeRepository likeRepository;
    private final CommentRepository commentRepository;
    private final RepostRepository repostRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final FileUploadService fileUploadService;
    private final ContentModerationService moderationService;

    @Autowired
    public CommunityServiceImpl(
            CommunityRepository communityRepository, UserRepository userRepository,
            PostRepository postRepository, CommunityMemberRepository communityMemberRepository,
            ProfileRepository profileRepository, LikeRepository likeRepository,
            CommentRepository commentRepository, RepostRepository repostRepository,
            ShareLinkRepository shareLinkRepository, FileUploadService fileUploadService,
            ContentModerationService moderationService) {
        this.communityRepository = communityRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.communityMemberRepository = communityMemberRepository;
        this.profileRepository = profileRepository;
        this.likeRepository = likeRepository;
        this.commentRepository = commentRepository;
        this.repostRepository = repostRepository;
        this.shareLinkRepository = shareLinkRepository;
        this.fileUploadService = fileUploadService;
        this.moderationService = moderationService;
    }

    @Override
    public Post createCommunityPost(UUID communityId, UUID userId, String title, String content,
                                    MultipartFile image, MultipartFile video) throws IOException {
        if (moderationService.isContentInappropriate(title) || moderationService.isContentInappropriate(content)) {
            throw new InappropriateContentException("O seu post não pôde ser publicado pois contém texto que viola as nossas diretrizes da comunidade.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Comunidade não encontrada!"));
        Profile profile = profileRepository.findByUser_UserId(userId).orElseGet(() -> {
            Profile created = new Profile();
            created.setUser(user);
            return profileRepository.save(created);
        });
        user.setProfile(profile);

        String imageUrl = null;
        String videoUrl = null;
        if (image != null && !image.isEmpty()) imageUrl = fileUploadService.uploadFile(image);
        if (video != null && !video.isEmpty()) videoUrl = fileUploadService.uploadFile(video);

        Post post = new Post();
        post.setUser(user);
        post.setProfile(profile);
        post.setCommunity(community);
        post.setTitle(title);
        post.setContent(content);
        post.setImageUrl(imageUrl);
        post.setVideoUrl(videoUrl);
        post.setCreatedAt(Instant.now());
        return postRepository.save(post);
    }
    
    @Override
    public Comment commentPost(UUID communityId, Long postId, CommentDTO commentDTO) {
        if (moderationService.isContentInappropriate(commentDTO.getContent())) {
            throw new InappropriateContentException("O seu comentário não pôde ser publicado pois contém texto que viola as nossas diretrizes da comunidade.");
        }
    
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(commentDTO.getUserId())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(commentDTO.getContent());
        comment.setCreatedAt(Instant.now());

        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        return commentRepository.save(comment);
    }
    
    // ... (o resto dos métodos da classe continua aqui, sem alterações) ...
    
    @Override
    @Transactional(readOnly = true)
    public List<CommunitySummaryDTO> getCommunities(int limit) {
        return communityRepository.findAll().stream()
                .limit(limit)
                .map(community -> new CommunitySummaryDTO(
                        community.getCommunityId(),
                        community.getName(),
                        community.getDescription(),
                        community.getMembers().size(),
                        community.getImageUrl(),
                        community.getOwner() == null ? null : community.getOwner().getUserId()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CommunitySummaryDTO createCommunity(CreateCommunityRequest request, UUID creatorId) {
        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        Community community = new Community();
        community.setName(request.name().trim());
        community.setDescription(request.description() == null ? null : request.description().trim());
        community.setOwner(creator);
        Community saved = communityRepository.save(community);

        // Quem cria a comunidade já entra como primeiro membro.
        joinCommunityWithMemberRepository(saved.getCommunityId(), creator.getUserId());
        return new CommunitySummaryDTO(saved.getCommunityId(), saved.getName(), saved.getDescription(), 1,
                saved.getImageUrl(), creator.getUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public CommunitySummaryDTO getCommunity(UUID communityId) {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new RuntimeException("Comunidade não encontrada!"));
        return new CommunitySummaryDTO(community.getCommunityId(), community.getName(), community.getDescription(),
            community.getMembers().size(), community.getImageUrl(), community.getOwner() == null ? null : community.getOwner().getUserId());
    }

    @Override
    @Transactional
    public void updateCommunityImage(UUID communityId, UUID userId, MultipartFile image) throws IOException {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new RuntimeException("Comunidade não encontrada!"));
        if (community.getOwner() == null || !community.getOwner().getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Somente quem criou a comunidade pode alterar a foto.");
        }
        if (image == null || image.isEmpty()) throw new IllegalArgumentException("Escolha uma imagem.");
        community.setImageUrl(fileUploadService.uploadFile(image));
        communityRepository.save(community);
    }

    @Override
    public Community joinCommunity(UUID communityId, UUID userId) {
        joinCommunityWithMemberRepository(communityId, userId);
        return communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Comunidade não encontrada!"));
    }

    private CommunityMember joinCommunityWithMemberRepository(UUID communityId, UUID userId) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Comunidade não encontrada com ID: " + communityId));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado com ID: " + userId));

        if (communityMemberRepository.existsById_CommunityIdAndId_UserId(communityId, userId)) {
            CommunityMemberId existingId = new CommunityMemberId();
            existingId.setCommunityId(communityId);
            existingId.setUserId(userId);
            return communityMemberRepository.findById(existingId).orElseThrow();
        }

        CommunityMember communityMember = new CommunityMember();
        CommunityMemberId id = new CommunityMemberId();
        id.setCommunityId(communityId);
        id.setUserId(userId);
        communityMember.setId(id);
        communityMember.setJoinedAt(Instant.now());

        return communityMemberRepository.save(communityMember);
    }

    @Override
    public List<Post> getCommunityPosts(UUID communityId) {
        return postRepository.findByCommunity_CommunityId(communityId);
    }

    @Override
    public Like likePost(UUID communityId, Long postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        if (likeRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) {
            throw new IllegalStateException("Usuário já curtiu este post!");
        }

        Like like = new Like(post, user);
        post.setLikesCount(post.getLikesCount() + 1);
        postRepository.save(post);

        return likeRepository.save(like);
    }

    @Override
    public Repost repostPost(UUID communityId, Long postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        if (repostRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) {
            throw new IllegalStateException("Usuário já repostou este post!");
        }

        Repost repost = new Repost(post, user);
        post.setRepostsCount(post.getRepostsCount() + 1);
        postRepository.save(post);

        return repostRepository.save(repost);
    }

    @Override
    public ShareLink sharePost(UUID communityId, Long postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        String shareUrl = "https://skillswap.com/share/post/" + UUID.randomUUID();

        ShareLink shareLink = new ShareLink();
        shareLink.setPost(post);
        shareLink.setUser(user);
        shareLink.setShareUrl(shareUrl);
        shareLink.setCreatedAt(Instant.now());
        
        post.setSharesCount(post.getSharesCount() + 1);
        postRepository.save(post);

        return shareLinkRepository.save(shareLink);
    }
}
