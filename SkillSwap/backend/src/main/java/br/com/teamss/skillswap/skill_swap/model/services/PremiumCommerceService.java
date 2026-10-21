package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.PremiumPurchaseResponse;
import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import br.com.teamss.skillswap.skill_swap.model.entities.LessonExtraPurchase;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.Proposal;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.LessonExtraPurchaseRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.LessonRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.PostRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProfileRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProposalRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

@Service
public class PremiumCommerceService {
    public static final long PROPOSAL_BOOST_COST = 20;
    public static final long POST_HIGHLIGHT_COST = 10;
    public static final long PORTFOLIO_SLOT_COST = 5;
    public static final long EXTRA_LESSON_HOUR_COST = 50;
    public static final long KARMA_SPONSORSHIP_COST = 5;
    public static final long BIO_LIMIT_COST = 2;
    public static final long POST_LIMIT_COST = 3;
    public static final int BIO_LIMIT_INCREMENT = 500;
    public static final int POST_LIMIT_INCREMENT = 1000;
    private final ProposalRepository proposalRepository;
    private final PostRepository postRepository;
    private final ProfileRepository profileRepository;
    private final LessonRepository lessonRepository;
    private final LessonExtraPurchaseRepository purchaseRepository;
    private final UserRepository userRepository;
    private final SwapCoinWalletService walletService;
    private final KarmaService karmaService;
    private final boolean commerceEnabled;

    public PremiumCommerceService(ProposalRepository proposalRepository, PostRepository postRepository,
                                  ProfileRepository profileRepository, LessonRepository lessonRepository,
                                  LessonExtraPurchaseRepository purchaseRepository, UserRepository userRepository,
                                  SwapCoinWalletService walletService, KarmaService karmaService,
                                  @Value("${app.commerce.enabled:false}") boolean commerceEnabled) {
        this.proposalRepository = proposalRepository;
        this.postRepository = postRepository;
        this.profileRepository = profileRepository;
        this.lessonRepository = lessonRepository;
        this.purchaseRepository = purchaseRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.karmaService = karmaService;
        this.commerceEnabled = commerceEnabled;
    }

    public Map<String, Object> catalog() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("mode", commerceEnabled ? "COMMERCE_ENABLED" : "FREE");
        result.put("paymentRequired", commerceEnabled);
        result.put("aiChatbot", Map.of("freeMonthlyQuota", 20, "costAfterQuota", commerceEnabled ? 1 : 0));
        result.put("proposalBoost", Map.of("cost", effectiveCost(PROPOSAL_BOOST_COST), "durationHours", 48));
        result.put("postHighlight", Map.of("cost", effectiveCost(POST_HIGHLIGHT_COST), "durationHours", 48));
        result.put("portfolioSlot", Map.of("cost", effectiveCost(PORTFOLIO_SLOT_COST)));
        result.put("extraLessonHour", Map.of("cost", effectiveCost(EXTRA_LESSON_HOUR_COST), "maxHoursPerPurchase", 10));
        result.put("karmaSponsorship", Map.of("cost", effectiveCost(KARMA_SPONSORSHIP_COST), "durationHours", 48));
        result.put("bioLimit", Map.of("cost", effectiveCost(BIO_LIMIT_COST), "characters", BIO_LIMIT_INCREMENT));
        result.put("postLimit", Map.of("cost", effectiveCost(POST_LIMIT_COST), "characters", POST_LIMIT_INCREMENT));
        return result;
    }

    @Transactional
    public PremiumPurchaseResponse boostProposal(UUID userId, long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId).orElseThrow(() -> new EntityNotFoundException("Proposta não encontrada."));
        requireOwner(userId, proposal.getSender());
        Instant now = Instant.now();
        Instant base = proposal.getBoostedUntil() != null && proposal.getBoostedUntil().isAfter(now)
            ? proposal.getBoostedUntil() : now;
        String key = "premium:proposal-boost:" + proposalId + ":" + now.toEpochMilli();
        debitIfEnabled(userId, PROPOSAL_BOOST_COST, "Impulsionamento de proposta #" + proposalId, key);
        proposal.setBoostedUntil(base.plus(Duration.ofHours(48)));
        proposalRepository.save(proposal);
        return new PremiumPurchaseResponse("PROPOSAL_BOOST", effectiveCost(PROPOSAL_BOOST_COST),
            walletService.getWallet(userId).balance(), null);
    }

    @Transactional
    public PremiumPurchaseResponse highlightPost(UUID userId, long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new EntityNotFoundException("Post não encontrado."));
        requireOwner(userId, post.getUser());
        Instant now = Instant.now();
        Instant base = post.getHighlightedUntil() != null && post.getHighlightedUntil().isAfter(now)
            ? post.getHighlightedUntil() : now;
        String key = "premium:post-highlight:" + postId + ":" + now.toEpochMilli();
        debitIfEnabled(userId, POST_HIGHLIGHT_COST, "Post destacado #" + postId, key);
        post.setHighlightedUntil(base.plus(Duration.ofHours(48)));
        postRepository.save(post);
        return new PremiumPurchaseResponse("POST_HIGHLIGHT", effectiveCost(POST_HIGHLIGHT_COST),
            walletService.getWallet(userId).balance(), null);
    }

    @Transactional
    public PremiumPurchaseResponse sponsorProposal(UUID sponsorId, long proposalId) {
        Proposal proposal = proposalRepository.findById(proposalId).orElseThrow(() -> new EntityNotFoundException("Proposta não encontrada."));
        Instant now = Instant.now();
        Instant base = proposal.getBoostedUntil() != null && proposal.getBoostedUntil().isAfter(now) ? proposal.getBoostedUntil() : now;
        String key = "premium:karma-proposal:" + proposalId + ":" + UUID.randomUUID();
        debitIfEnabled(sponsorId, KARMA_SPONSORSHIP_COST, "Patrocínio Karma da proposta #" + proposalId, key);
        proposal.setBoostedUntil(base.plus(Duration.ofHours(48)));
        proposalRepository.save(proposal);
        karmaService.award(sponsorId, 1, "Apoiou uma proposta com Karma", "karma:sponsor:proposal:sponsor:" + key);
        karmaService.award(proposal.getSender().getUserId(), 10, "Recebeu patrocinio Karma em uma proposta", "karma:sponsor:proposal:owner:" + key);
        return new PremiumPurchaseResponse("KARMA_PROPOSAL", effectiveCost(KARMA_SPONSORSHIP_COST),
            walletService.getWallet(sponsorId).balance(), null);
    }

    @Transactional
    public PremiumPurchaseResponse sponsorPost(UUID sponsorId, long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new EntityNotFoundException("Post não encontrado."));
        Instant now = Instant.now();
        Instant base = post.getHighlightedUntil() != null && post.getHighlightedUntil().isAfter(now) ? post.getHighlightedUntil() : now;
        String key = "premium:karma-post:" + postId + ":" + UUID.randomUUID();
        debitIfEnabled(sponsorId, KARMA_SPONSORSHIP_COST, "Patrocínio Karma do post #" + postId, key);
        post.setHighlightedUntil(base.plus(Duration.ofHours(48)));
        postRepository.save(post);
        karmaService.award(sponsorId, 1, "Apoiou um post com Karma", "karma:sponsor:post:sponsor:" + key);
        karmaService.award(post.getUser().getUserId(), 10, "Recebeu patrocinio Karma em um post", "karma:sponsor:post:owner:" + key);
        return new PremiumPurchaseResponse("KARMA_POST", effectiveCost(KARMA_SPONSORSHIP_COST),
            walletService.getWallet(sponsorId).balance(), null);
    }

    @Transactional
    public PremiumPurchaseResponse addPortfolioSlot(UUID userId) {
        Profile profile = profileRepository.findByUser_UserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Perfil não encontrado."));
        String key = "premium:portfolio-slot:" + userId + ":" + UUID.randomUUID();
        debitIfEnabled(userId, PORTFOLIO_SLOT_COST, "Slot extra de portfólio", key);
        profile.setExtraPortfolioSlots(profile.getExtraPortfolioSlots() + 1);
        profileRepository.save(profile);
        return new PremiumPurchaseResponse("PORTFOLIO_SLOT", effectiveCost(PORTFOLIO_SLOT_COST),
            walletService.getWallet(userId).balance(), null);
    }

    @Transactional
    public PremiumPurchaseResponse addBioLimit(UUID userId) {
        Profile profile = profileRepository.findByUser_UserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Perfil nao encontrado."));
        String key = "premium:bio-limit:" + userId + ":" + UUID.randomUUID();
        debitIfEnabled(userId, BIO_LIMIT_COST, "Aumento do limite da biografia", key);
        profile.setExtraBioChars(profile.getExtraBioChars() + BIO_LIMIT_INCREMENT);
        profileRepository.save(profile);
        return new PremiumPurchaseResponse("BIO_LIMIT", effectiveCost(BIO_LIMIT_COST), walletService.getWallet(userId).balance(), null);
    }

    @Transactional
    public PremiumPurchaseResponse addPostLimit(UUID userId) {
        Profile profile = profileRepository.findByUser_UserId(userId)
            .orElseThrow(() -> new EntityNotFoundException("Perfil nao encontrado."));
        String key = "premium:post-limit:" + userId + ":" + UUID.randomUUID();
        debitIfEnabled(userId, POST_LIMIT_COST, "Aumento do limite das postagens", key);
        profile.setExtraPostChars(profile.getExtraPostChars() + POST_LIMIT_INCREMENT);
        profileRepository.save(profile);
        return new PremiumPurchaseResponse("POST_LIMIT", effectiveCost(POST_LIMIT_COST), walletService.getWallet(userId).balance(), null);
    }

    @Transactional
    public PremiumPurchaseResponse buyExtraLesson(UUID userId, long lessonId, int hours) {
        if (hours < 1 || hours > 10) throw new IllegalArgumentException("A extensão de aula deve ter entre 1 e 10 horas.");
        Lesson lesson = lessonRepository.findById(lessonId).orElseThrow(() -> new EntityNotFoundException("Aula não encontrada."));
        requireOwner(userId, lesson.getStudent());
        long amount = Math.multiplyExact(EXTRA_LESSON_HOUR_COST, hours);
        User teacher = lesson.getTeacher();
        String key = "premium:lesson-extra:" + lessonId + ":" + UUID.randomUUID();
        LessonExtraPurchase purchase = new LessonExtraPurchase();
        purchase.setLesson(lesson);
        purchase.setPayer(lesson.getStudent());
        purchase.setTeacher(teacher);
        purchase.setHours(hours);
        purchase.setAmount(amount);
        purchase.setIdempotencyKey(key);
        if (commerceEnabled) {
            purchase = purchaseRepository.saveAndFlush(purchase);
            walletService.payLessonExtra(userId, teacher.getUserId(), amount,
                "Aula extra #" + lessonId + " (" + hours + "h)", key);
        }
        return new PremiumPurchaseResponse("EXTRA_LESSON", effectiveCost(amount),
            walletService.getWallet(userId).balance(), commerceEnabled ? purchase.getPurchaseId() : null);
    }

    private long effectiveCost(long amount) {
        return commerceEnabled ? amount : 0L;
    }

    private void debitIfEnabled(UUID userId, long amount, String description, String key) {
        if (commerceEnabled) walletService.debitForFeature(userId, amount, description, key);
    }

    private void requireOwner(UUID userId, User owner) {
        if (owner == null || !userId.equals(owner.getUserId())) {
            throw new org.springframework.security.access.AccessDeniedException("Você não pode monetizar este recurso.");
        }
    }
}
