package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.model.premium.PremiumCharge;
import br.com.teamss.skillswap.skill_swap.model.premium.PremiumUsage;
import br.com.teamss.skillswap.skill_swap.model.repositories.PremiumUsageRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

/** Applies monthly freemium allowances and records paid consumption in the ledger. */
@Service
public class PremiumFeatureService {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("America/Sao_Paulo");
    private final PremiumUsageRepository usageRepository;
    private final UserRepository userRepository;
    private final SwapCoinWalletService walletService;
    private final boolean commerceEnabled;

    public PremiumFeatureService(PremiumUsageRepository usageRepository, UserRepository userRepository,
                                 SwapCoinWalletService walletService,
                                 @Value("${app.commerce.enabled:false}") boolean commerceEnabled) {
        this.usageRepository = usageRepository;
        this.userRepository = userRepository;
        this.walletService = walletService;
        this.commerceEnabled = commerceEnabled;
    }

    @Transactional
    public PremiumCharge consume(UUID userId, String featureCode, long cost, int freeQuota) {
        if (cost < 0 || freeQuota < 0) throw new IllegalArgumentException("Política premium inválida.");
        LocalDate period = LocalDate.now(BUSINESS_ZONE).withDayOfMonth(1);
        PremiumUsage usage = usageRepository.findForUpdate(userId, period).orElseGet(() -> {
            PremiumUsage created = new PremiumUsage();
            created.setOwner(userRepository.findById(userId).orElseThrow());
            created.setPeriodStart(period);
            return usageRepository.saveAndFlush(created);
        });
        if ("CHATBOT_QUERY".equals(featureCode) && usage.getFreeAiPromptsUsed() < freeQuota) {
            usage.setFreeAiPromptsUsed(usage.getFreeAiPromptsUsed() + 1);
            usage.touch();
            usageRepository.save(usage);
            return PremiumCharge.free(userId, featureCode);
        }
        // O modo gratuito mantém a telemetria de uso, mas nunca debita
        // SwapCoin nem exige pagamento do usuário.
        if (!commerceEnabled || cost == 0) return PremiumCharge.free(userId, featureCode);
        String key = "premium:" + featureCode + ":" + UUID.randomUUID();
        walletService.debitForFeature(userId, cost, "Uso premium: " + featureCode, key);
        return new PremiumCharge(userId, featureCode, cost, true, key);
    }

    @Transactional
    public void refund(PremiumCharge charge) {
        if (charge != null && charge.charged()) {
            walletService.refundFeature(charge.userId(), charge.amount(),
                "Estorno de uso premium: " + charge.featureCode(), charge.idempotencyKey() + ":refund");
        }
    }

    @Transactional(readOnly = true)
    public java.util.Map<String, Object> getAiQuota(UUID userId, int monthlyQuota) {
        LocalDate period = LocalDate.now(BUSINESS_ZONE).withDayOfMonth(1);
        int used = usageRepository.findByOwner_UserIdAndPeriodStart(userId, period)
            .map(PremiumUsage::getFreeAiPromptsUsed).orElse(0);
        return java.util.Map.of("periodStart", period, "monthlyQuota", monthlyQuota,
            "used", used, "remaining", Math.max(0, monthlyQuota - used),
            "paidCostAfterQuota", commerceEnabled ? 1 : 0,
            "mode", commerceEnabled ? "COMMERCE_ENABLED" : "FREE");
    }
}
