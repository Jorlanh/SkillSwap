package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.PlatformStatsDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserManagementDTO;
import br.com.teamss.skillswap.skill_swap.dto.AdminOperationsReportDTO;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinLedgerResponse;
import br.com.teamss.skillswap.skill_swap.dto.AdminWalletAdjustmentRequest;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinWalletResponse;
import br.com.teamss.skillswap.skill_swap.model.entities.Ban;
import br.com.teamss.skillswap.skill_swap.model.entities.Role;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.BanRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.RoleRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProposalRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SkillRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SwapCoinLedgerRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SwapCoinWalletRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SwapCoinPaymentRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.PremiumUsageRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.LessonExtraPurchaseRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.KarmaEventRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.PostRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProfileRepository;
import br.com.teamss.skillswap.skill_swap.model.wallet.LedgerEntryType;
import br.com.teamss.skillswap.skill_swap.model.wallet.PaymentStatus;
import br.com.teamss.skillswap.skill_swap.model.services.SwapCoinWalletService;
import br.com.teamss.skillswap.skill_swap.model.services.AdminService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.beans.factory.annotation.Value;

@Service
public class AdminServiceImpl implements AdminService {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final ProposalRepository proposalRepository;
    private final BanRepository banRepository;
    private final RoleRepository roleRepository;
    private final SwapCoinLedgerRepository ledgerRepository;
    private final SwapCoinWalletRepository walletRepository;
    private final SwapCoinPaymentRepository paymentRepository;
    private final PremiumUsageRepository premiumUsageRepository;
    private final LessonExtraPurchaseRepository lessonPurchaseRepository;
    private final KarmaEventRepository karmaEventRepository;
    private final PostRepository postRepository;
    private final ProfileRepository profileRepository;
    private final boolean rabbitMqEnabled;
    private final SwapCoinWalletService walletService;

    public AdminServiceImpl(UserRepository userRepository, SkillRepository skillRepository,
                            ProposalRepository proposalRepository, BanRepository banRepository, RoleRepository roleRepository,
                            SwapCoinLedgerRepository ledgerRepository, SwapCoinWalletRepository walletRepository,
                            SwapCoinPaymentRepository paymentRepository, PremiumUsageRepository premiumUsageRepository,
                            LessonExtraPurchaseRepository lessonPurchaseRepository, KarmaEventRepository karmaEventRepository,
                            PostRepository postRepository, ProfileRepository profileRepository,
                            @Value("${app.messaging.rabbitmq.enabled:false}") boolean rabbitMqEnabled,
                            SwapCoinWalletService walletService) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.proposalRepository = proposalRepository;
        this.banRepository = banRepository;
        this.roleRepository = roleRepository;
        this.ledgerRepository = ledgerRepository;
        this.walletRepository = walletRepository;
        this.paymentRepository = paymentRepository;
        this.premiumUsageRepository = premiumUsageRepository;
        this.lessonPurchaseRepository = lessonPurchaseRepository;
        this.karmaEventRepository = karmaEventRepository;
        this.postRepository = postRepository;
        this.profileRepository = profileRepository;
        this.rabbitMqEnabled = rabbitMqEnabled;
        this.walletService = walletService;
    }

    @Override
    public List<UserManagementDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToUserManagementDTO)
                .collect(Collectors.toList());
    }

    @Override
    public UserManagementDTO toggleUserVerification(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + userId));
        
        user.setVerifiedBadge(!user.isVerifiedBadge());
        User updatedUser = userRepository.save(user);

        return convertToUserManagementDTO(updatedUser);
    }

    @Override
    public PlatformStatsDTO getPlatformStatistics() {
        long totalUsers = userRepository.count();
        long totalSkills = skillRepository.count();
        long totalProposals = proposalRepository.count();
        
        long pendingProposals = proposalRepository.countByStatus("PENDING");
        long completedProposals = proposalRepository.countByStatus("COMPLETED");

        return new PlatformStatsDTO(totalUsers, totalSkills, totalProposals, pendingProposals, completedProposals);
    }

    @Override
    public void banUser(UUID userId, String reason, Instant expiresAt, String ipAddress) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + userId));

        user.setBanned(true);
        userRepository.save(user);

        Ban ban = new Ban();
        ban.setUser(user);
        ban.setReason(reason);
        ban.setExpiresAt(expiresAt);
        ban.setIpAddress(ipAddress);
        banRepository.save(ban);
    }

    @Override
    public void unbanUser(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + userId));
        
        user.setBanned(false);
        userRepository.save(user);

        List<Ban> activeBans = banRepository.findByUser_UserIdAndActiveTrue(userId);
        activeBans.forEach(ban -> ban.setActive(false));
        banRepository.saveAll(activeBans);
    }

    @Override
    public UserManagementDTO setRole(UUID userId, String roleName, boolean enabled) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + userId));
        String normalizedRole = roleName.trim().toUpperCase();
        Role role = roleRepository.findByNameIgnoreCase(normalizedRole)
            .orElseGet(() -> roleRepository.save(new Role(null, normalizedRole)));
        if (enabled) user.getRoles().add(role);
        else user.getRoles().removeIf(current -> normalizedRole.equalsIgnoreCase(current.getName()));
        return convertToUserManagementDTO(userRepository.save(user));
    }

    @Override
    public AdminOperationsReportDTO getOperationsReport() {
        return new AdminOperationsReportDTO(
            walletRepository.countWallets(), walletRepository.sumBalances(),
            paymentRepository.countByStatus(PaymentStatus.PAID), paymentRepository.countByStatus(PaymentStatus.PENDING),
            paymentRepository.countByStatus(PaymentStatus.FAILED), paymentRepository.sumAmountCentsByStatus(PaymentStatus.PAID),
            paymentRepository.sumCoinAmountByStatus(PaymentStatus.PAID), ledgerRepository.count(), ledgerRepository.sumCredits(),
            ledgerRepository.sumDebits(), ledgerRepository.sumDebitsByType(LedgerEntryType.FEATURE_SPEND),
            ledgerRepository.sumDebitsByType(LedgerEntryType.TIP_SENT) + ledgerRepository.sumCreditsByType(LedgerEntryType.TIP_RECEIVED),
            ledgerRepository.sumCreditsByType(LedgerEntryType.LESSON_PAYMENT), ledgerRepository.sumCreditsByType(LedgerEntryType.ACHIEVEMENT_REWARD),
            premiumUsageRepository.sumFreeAiPromptsUsed(), lessonPurchaseRepository.countByStatus(br.com.teamss.skillswap.skill_swap.model.entities.LessonExtraPurchase.Status.PAID),
            lessonPurchaseRepository.sumPaidAmount(br.com.teamss.skillswap.skill_swap.model.entities.LessonExtraPurchase.Status.PAID), karmaEventRepository.sumPoints(),
            proposalRepository.findAll().stream().filter(p -> p.getBoostedUntil() != null && p.getBoostedUntil().isAfter(Instant.now())).count(),
            postRepository.findAll().stream().filter(p -> p.getHighlightedUntil() != null && p.getHighlightedUntil().isAfter(Instant.now())).count(),
            profileRepository.findAll().stream().mapToLong(p -> p.getExtraPortfolioSlots()).sum(), rabbitMqEnabled);
    }

    @Override
    public Page<SwapCoinLedgerResponse> getLedgerAudit(Pageable pageable) {
        return ledgerRepository.findAll(pageable).map(SwapCoinLedgerResponse::from);
    }

    @Override
    public SwapCoinWalletResponse adjustWallet(UUID userId, AdminWalletAdjustmentRequest request) {
        return walletService.adjustBalance(userId, request.amount(), request.reason(), request.idempotencyKey());
    }

    private UserManagementDTO convertToUserManagementDTO(User user) {
        return new UserManagementDTO(
                user.getUserId(),
                user.getUsername(),
                user.getName(),
                user.getEmail(),
                user.isVerifiedBadge(),
                user.getRoles().stream().map(Role::getName).collect(Collectors.toSet()),
                user.isBanned(),
                user.getCreatedAt(),
                walletRepository.findByOwner_UserId(user.getUserId()).map(wallet -> wallet.getBalance()).orElse(0L),
                user.getKarmaPoints()
        );
    }
}
