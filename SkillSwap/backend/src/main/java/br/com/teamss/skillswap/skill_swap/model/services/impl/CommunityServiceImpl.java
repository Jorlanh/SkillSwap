package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.CommentDTO;
import br.com.teamss.skillswap.skill_swap.dto.CommunitySummaryDTO;
import br.com.teamss.skillswap.skill_swap.dto.CreateCommunityRequest;
import br.com.teamss.skillswap.skill_swap.dto.UpdateCommunityRequest;
import br.com.teamss.skillswap.skill_swap.model.entities.*;
import br.com.teamss.skillswap.skill_swap.model.exception.InappropriateContentException;
import br.com.teamss.skillswap.skill_swap.model.repositories.*;
import br.com.teamss.skillswap.skill_swap.model.services.CommunityService;
import br.com.teamss.skillswap.skill_swap.model.services.ContentModerationService;
import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

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
        ensureApprovedMember(communityId, userId);
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
        ensureApprovedMember(communityId, user.getUserId());

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(commentDTO.getContent());
        comment.setCreatedAt(Instant.now());
        if (commentDTO.getParentCommentId() != null) {
            Comment parent = commentRepository.findById(commentDTO.getParentCommentId())
                    .orElseThrow(() -> new RuntimeException("Comentário principal não encontrado."));
            if (!parent.getPost().getPostId().equals(postId)) throw new IllegalArgumentException("A resposta pertence a outro post.");
            comment.setParentComment(parent);
        }

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
                        (int) communityMemberRepository.countById_CommunityIdAndMembershipStatus(community.getCommunityId(), "APPROVED"),
                        community.getImageUrl(),
                        community.getOwner() == null ? null : community.getOwner().getUserId(),
                        community.isPrivateCommunity(), false, null))
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
        community.setPrivateCommunity(request.privateCommunity());
        Community saved = communityRepository.save(community);

        // Quem cria a comunidade já entra como primeiro membro.
        joinCommunityWithMemberRepository(saved.getCommunityId(), creator.getUserId());
        return new CommunitySummaryDTO(saved.getCommunityId(), saved.getName(), saved.getDescription(), 1,
                saved.getImageUrl(), creator.getUserId(), saved.isPrivateCommunity(), true, "OWNER");
    }

    @Override
    @Transactional(readOnly = true)
    public CommunitySummaryDTO getCommunity(UUID communityId) {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new RuntimeException("Comunidade não encontrada!"));
        int memberCount = (int) communityMemberRepository.countById_CommunityIdAndMembershipStatus(communityId, "APPROVED");
        return new CommunitySummaryDTO(community.getCommunityId(), community.getName(), community.getDescription(),
            memberCount, community.getImageUrl(), community.getOwner() == null ? null : community.getOwner().getUserId(),
            community.isPrivateCommunity(), false, null);
    }

    @Override
    @Transactional
    public CommunitySummaryDTO updateCommunity(UUID communityId, UUID userId, UpdateCommunityRequest request) {
        Community community = communityRepository.findById(communityId)
                .orElseThrow(() -> new RuntimeException("Comunidade não encontrada!"));
        ensureCommunityManager(communityId, userId);
        if (request.name() != null && !request.name().isBlank()) community.setName(request.name().trim());
        if (request.description() != null) community.setDescription(request.description().trim());
        if (request.privateCommunity() != null) community.setPrivateCommunity(request.privateCommunity());
        Community saved = communityRepository.save(community);
        return summaryForViewer(saved, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public String getMembershipStatus(UUID communityId, UUID userId) {
        return communityMemberRepository.findById_CommunityIdAndId_UserId(communityId, userId)
                .map(member -> {
                    if (!"APPROVED".equals(member.getMembershipStatus())) return member.getMembershipStatus();
                    if ("OWNER".equals(member.getMemberRole()) || "ADMIN".equals(member.getMemberRole()) || "MODERATOR".equals(member.getMemberRole())) {
                        return member.getMemberRole();
                    }
                    return "APPROVED";
                })
                .orElse("NONE");
    }

    @Override
    @Transactional
    public void updateCommunityImage(UUID communityId, UUID userId, MultipartFile image) throws IOException {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new RuntimeException("Comunidade não encontrada!"));
        ensureCommunityManager(communityId, userId);
        if (image == null || image.isEmpty()) throw new IllegalArgumentException("Escolha uma imagem.");
        community.setImageUrl(fileUploadService.uploadFile(image));
        communityRepository.save(community);
    }

    @Override
    @Transactional
    public Community joinCommunity(UUID communityId, UUID userId) {
        CommunityMember member = joinCommunityWithMemberRepository(communityId, userId);
        if ("APPROVED".equals(member.getMembershipStatus())) {
            User user = userRepository.findById(userId).orElseThrow();
            if (!user.getCommunityIds().contains(communityId)) {
                user.getCommunityIds().add(communityId);
                userRepository.save(user);
            }
        }
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
        communityMember.setMemberRole(community.getOwner() != null && community.getOwner().getUserId().equals(userId) ? "OWNER" : "MEMBER");
        communityMember.setMembershipStatus(community.isPrivateCommunity() && !community.getOwner().getUserId().equals(userId) ? "PENDING" : "APPROVED");

        return communityMemberRepository.save(communityMember);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Post> getCommunityPosts(UUID communityId) {
        return postRepository.findByCommunity_CommunityId(communityId);
    }

    @Override
    public Like likePost(UUID communityId, Long postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));
        ensureApprovedMember(communityId, userId);

        if (likeRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) {
            likeRepository.deleteByPost_PostIdAndUser_UserId(postId, userId);
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
            postRepository.save(post);
            return null;
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
        ensureApprovedMember(communityId, userId);

        if (repostRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) {
            repostRepository.deleteByPost_PostIdAndUser_UserId(postId, userId);
            post.setRepostsCount(Math.max(0, post.getRepostsCount() - 1));
            postRepository.save(post);
            return null;
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
        ensureApprovedMember(communityId, userId);

        String shareUrl = frontendUrl.replaceAll("/$", "") + "/post/" + postId;

        ShareLink shareLink = new ShareLink();
        shareLink.setPost(post);
        shareLink.setUser(user);
        shareLink.setShareUrl(shareUrl);
        shareLink.setCreatedAt(Instant.now());
        
        post.setSharesCount(post.getSharesCount() + 1);
        postRepository.save(post);

        return shareLinkRepository.save(shareLink);
    }

    @Override
    @Transactional(readOnly = true)
    public List<br.com.teamss.skillswap.skill_swap.dto.CommunityMemberDTO> listMembers(UUID communityId, UUID actorId, String search) {
        ensureModerator(communityId, actorId);
        return toMemberDtos(communityMemberRepository.findById_CommunityIdAndMembershipStatusOrderByJoinedAtAsc(communityId, "APPROVED"), search);
    }

    @Override
    @Transactional(readOnly = true)
    public List<br.com.teamss.skillswap.skill_swap.dto.CommunityMemberDTO> listPendingMembers(UUID communityId, UUID actorId, String search) {
        ensureModerator(communityId, actorId);
        return toMemberDtos(communityMemberRepository.findById_CommunityIdAndMembershipStatusOrderByJoinedAtAsc(communityId, "PENDING"), search);
    }

    private List<br.com.teamss.skillswap.skill_swap.dto.CommunityMemberDTO> toMemberDtos(List<CommunityMember> source, String search) {
        String normalized = search == null ? "" : search.trim().toLowerCase();
        return source.stream()
            .filter(member -> member.getUser() != null && (normalized.isBlank()
                || member.getUser().getUsername().toLowerCase().contains(normalized)
                || (member.getUser().getName() != null && member.getUser().getName().toLowerCase().contains(normalized))))
            .map(member -> new br.com.teamss.skillswap.skill_swap.dto.CommunityMemberDTO(
                member.getUser().getUserId(), member.getUser().getUsername(), member.getUser().getName(),
                member.getUser().getProfile() == null ? null : member.getUser().getProfile().getImageUrl(),
                member.getMemberRole(), member.getMembershipStatus()))
            .toList();
    }

    @Override
    @Transactional
    public void removeMember(UUID communityId, UUID actorId, UUID memberId) {
        ensureModerator(communityId, actorId);
        Community community = communityRepository.findById(communityId).orElseThrow();
        if (community.getOwner() != null && community.getOwner().getUserId().equals(memberId)) throw new IllegalArgumentException("O criador não pode ser removido.");
        CommunityMember target = communityMemberRepository.findById_CommunityIdAndId_UserId(communityId, memberId)
            .orElseThrow(() -> new IllegalArgumentException("Membro não encontrado."));
        if ("ADMIN".equals(target.getMemberRole()) && !isCommunityOwner(communityId, actorId)) {
            throw new org.springframework.security.access.AccessDeniedException("Somente o dono pode remover um administrador.");
        }
        CommunityMemberId id = new CommunityMemberId(); id.setCommunityId(communityId); id.setUserId(memberId);
        communityMemberRepository.deleteById(id);
        userRepository.findById(memberId).ifPresent(user -> {
            user.getCommunityIds().removeIf(communityId::equals);
            userRepository.save(user);
        });
    }

    @Override
    @Transactional
    public void setAdministrator(UUID communityId, UUID actorId, UUID memberId, boolean administrator) {
        ensureCommunityOwner(communityId, actorId);
        Community community = communityRepository.findById(communityId).orElseThrow();
        if (community.getOwner() != null && community.getOwner().getUserId().equals(memberId)) return;
        CommunityMember member = communityMemberRepository.findById_CommunityIdAndId_UserId(communityId, memberId).orElseThrow(() -> new IllegalArgumentException("Membro não encontrado."));
        member.setMemberRole(administrator ? "ADMIN" : "MEMBER");
        communityMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void setModerator(UUID communityId, UUID actorId, UUID memberId, boolean moderator) {
        ensureCommunityManager(communityId, actorId);
        Community community = communityRepository.findById(communityId).orElseThrow();
        if (community.getOwner() != null && community.getOwner().getUserId().equals(memberId)) return;
        CommunityMember member = communityMemberRepository.findById_CommunityIdAndId_UserId(communityId, memberId)
            .orElseThrow(() -> new IllegalArgumentException("Membro não encontrado."));
        if ("ADMIN".equals(member.getMemberRole()) && !isCommunityOwner(communityId, actorId)) {
            throw new org.springframework.security.access.AccessDeniedException("Somente o dono pode alterar o papel de um administrador.");
        }
        member.setMemberRole(moderator ? "MODERATOR" : "MEMBER");
        communityMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void approveMember(UUID communityId, UUID actorId, UUID memberId) {
        ensureCommunityManager(communityId, actorId);
        CommunityMember member = communityMemberRepository.findById_CommunityIdAndId_UserId(communityId, memberId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada."));
        member.setMembershipStatus("APPROVED");
        communityMemberRepository.save(member);
        User user = userRepository.findById(memberId).orElseThrow();
        if (!user.getCommunityIds().contains(communityId)) {
            user.getCommunityIds().add(communityId);
            userRepository.save(user);
        }
    }

    @Override
    @Transactional
    public void deleteCommunity(UUID communityId, UUID actorId) {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new RuntimeException("Comunidade não encontrada."));
        ensureCommunityOwner(communityId, actorId);
        deleteCommunityData(community);
    }

    @Override
    @Transactional
    public void deleteCommunityAsPlatformAdmin(UUID communityId) {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new RuntimeException("Comunidade não encontrada."));
        deleteCommunityData(community);
    }

    @Override
    @Transactional
    public void transferOwnership(UUID communityId, UUID actorId, UUID newOwnerId) {
        ensureCommunityOwner(communityId, actorId);
        transferOwnershipInternal(communityId, newOwnerId);
    }

    @Override
    @Transactional
    public void transferOwnershipAsPlatformAdmin(UUID communityId, UUID newOwnerId) {
        transferOwnershipInternal(communityId, newOwnerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<br.com.teamss.skillswap.skill_swap.dto.CommunityMemberDTO> listMembersAsPlatformAdmin(UUID communityId, String search) {
        if (!communityRepository.existsById(communityId)) throw new RuntimeException("Comunidade não encontrada.");
        return toMemberDtos(communityMemberRepository.findById_CommunityIdAndMembershipStatusOrderByJoinedAtAsc(communityId, "APPROVED"), search);
    }

    @Override
    @Transactional
    public void handlePermanentOwnerBan(UUID bannedUserId) {
        for (Community community : communityRepository.findByOwner_UserId(bannedUserId)) {
            List<CommunityMember> approved = communityMemberRepository
                .findById_CommunityIdAndMembershipStatusOrderByJoinedAtAsc(community.getCommunityId(), "APPROVED");
            CommunityMember successor = approved.stream()
                .filter(member -> !bannedUserId.equals(member.getId().getUserId()) && "ADMIN".equals(member.getMemberRole()))
                .findFirst()
                .orElseGet(() -> approved.stream()
                    .filter(member -> !bannedUserId.equals(member.getId().getUserId()))
                    .findFirst().orElse(null));
            if (successor == null) continue;
            CommunityMember formerOwner = communityMemberRepository
                .findById_CommunityIdAndId_UserId(community.getCommunityId(), bannedUserId).orElse(null);
            if (formerOwner != null) formerOwner.setMemberRole("MEMBER");
            successor.setMemberRole("OWNER");
            community.setOwner(successor.getUser());
            if (formerOwner != null) communityMemberRepository.save(formerOwner);
            communityMemberRepository.save(successor);
            communityRepository.save(community);
        }
    }

    private void transferOwnershipInternal(UUID communityId, UUID newOwnerId) {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new RuntimeException("Comunidade não encontrada."));
        User newOwner = userRepository.findById(newOwnerId)
            .orElseThrow(() -> new IllegalArgumentException("O novo dono não foi encontrado."));
        CommunityMember target = communityMemberRepository.findById_CommunityIdAndId_UserId(communityId, newOwnerId)
            .orElseThrow(() -> new IllegalArgumentException("O novo dono precisa participar da comunidade."));
        if (!"APPROVED".equals(target.getMembershipStatus())) {
            throw new IllegalArgumentException("O novo dono precisa ser um membro aprovado.");
        }
        if (community.getOwner() != null) {
            communityMemberRepository.findById_CommunityIdAndId_UserId(communityId, community.getOwner().getUserId())
                .ifPresent(previous -> { previous.setMemberRole("ADMIN"); communityMemberRepository.save(previous); });
        }
        target.setMemberRole("OWNER");
        community.setOwner(newOwner);
        communityMemberRepository.save(target);
        communityRepository.save(community);
    }

    private void deleteCommunityData(Community community) {
        UUID communityId = community.getCommunityId();
        // Os posts da comunidade continuam sendo publicações da plataforma.
        // Desvinculá-los antes de apagar a comunidade evita quebrar a FK e
        // preserva o conteúdo publicado pelos membros.
        List<Post> communityPosts = postRepository.findByCommunity_CommunityId(communityId);
        for (Post post : communityPosts) {
            post.setCommunity(null);
        }
        postRepository.saveAll(communityPosts);

        List<CommunityMember> members = communityMemberRepository.findById_CommunityId(communityId);
        for (CommunityMember member : members) {
            userRepository.findById(member.getId().getUserId()).ifPresent(user -> {
                user.getCommunityIds().removeIf(communityId::equals);
                userRepository.save(user);
            });
        }
        // Exclusão em lote evita que o Hibernate tente remover uma entidade
        // CommunityMember já removida pela tabela de associação legada.
        communityMemberRepository.deleteAllByCommunityId(communityId);
        communityRepository.delete(community);
    }

    private CommunitySummaryDTO summaryForViewer(Community community, UUID viewerId) {
        String status = viewerId == null ? null : communityMemberRepository
            .findById_CommunityIdAndId_UserId(community.getCommunityId(), viewerId)
            .map(CommunityMember::getMembershipStatus).orElse("NONE");
        boolean joined = "APPROVED".equals(status) || "OWNER".equals(status) || "ADMIN".equals(status) || "MODERATOR".equals(status);
        return new CommunitySummaryDTO(community.getCommunityId(), community.getName(), community.getDescription(),
            (int) communityMemberRepository.countById_CommunityIdAndMembershipStatus(community.getCommunityId(), "APPROVED"),
            community.getImageUrl(), community.getOwner() == null ? null : community.getOwner().getUserId(),
            community.isPrivateCommunity(), joined, status);
    }

    private boolean isCommunityOwner(UUID communityId, UUID actorId) {
        boolean ownerColumnMatches = communityRepository.findById(communityId)
            .map(community -> community.getOwner() != null && actorId.equals(community.getOwner().getUserId()))
            .orElse(false);
        if (ownerColumnMatches) return true;

        // Compatibilidade com comunidades antigas: nelas o papel OWNER pode
        // estar correto na tabela de membros mesmo quando owner_id ficou nulo
        // ou desatualizado.
        return communityMemberRepository.findById_CommunityIdAndId_UserId(communityId, actorId)
            .map(member -> "OWNER".equalsIgnoreCase(member.getMemberRole())
                && "APPROVED".equalsIgnoreCase(member.getMembershipStatus()))
            .orElse(false);
    }

    private void ensureCommunityOwner(UUID communityId, UUID actorId) {
        if (!isCommunityOwner(communityId, actorId)) {
            throw new org.springframework.security.access.AccessDeniedException("Somente o dono da comunidade pode realizar esta ação.");
        }
    }

    private void ensureCommunityManager(UUID communityId, UUID actorId) {
        Community community = communityRepository.findById(communityId)
            .orElseThrow(() -> new RuntimeException("Comunidade não encontrada."));
        if (isCommunityOwner(communityId, actorId)) return;
        CommunityMember member = communityMemberRepository.findById_CommunityIdAndId_UserId(communityId, actorId)
            .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Você não administra esta comunidade."));
        if (!"ADMIN".equals(member.getMemberRole()) || !"APPROVED".equals(member.getMembershipStatus())) {
            throw new org.springframework.security.access.AccessDeniedException("Você não administra esta comunidade.");
        }
    }

    private void ensureModerator(UUID communityId, UUID actorId) {
        Community community = communityRepository.findById(communityId).orElseThrow(() -> new RuntimeException("Comunidade não encontrada."));
        if (isCommunityOwner(communityId, actorId)) return;
        CommunityMember member = communityMemberRepository.findById_CommunityIdAndId_UserId(communityId, actorId).orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Você não administra esta comunidade."));
        if (!("ADMIN".equals(member.getMemberRole()) || "MODERATOR".equals(member.getMemberRole())) || !"APPROVED".equals(member.getMembershipStatus())) throw new org.springframework.security.access.AccessDeniedException("Você não administra esta comunidade.");
    }

    private void ensureApprovedMember(UUID communityId, UUID userId) {
        CommunityMember member = communityMemberRepository.findById_CommunityIdAndId_UserId(communityId, userId)
                .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Entre na comunidade antes de publicar."));
        if (!"APPROVED".equals(member.getMembershipStatus())) {
            throw new org.springframework.security.access.AccessDeniedException("Sua entrada na comunidade ainda aguarda aprovação.");
        }
    }
}
