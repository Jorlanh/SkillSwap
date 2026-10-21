package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.CreateCheckoutSessionResponse;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinLedgerResponse;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinTransferRequest;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinWalletResponse;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.SwapCoinLedgerRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SwapCoinPaymentRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SwapCoinWalletRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.wallet.LedgerEntryType;
import br.com.teamss.skillswap.skill_swap.model.wallet.PaymentStatus;
import br.com.teamss.skillswap.skill_swap.model.wallet.SwapCoinLedgerEntry;
import br.com.teamss.skillswap.skill_swap.model.wallet.SwapCoinPackage;
import br.com.teamss.skillswap.skill_swap.model.wallet.SwapCoinPayment;
import br.com.teamss.skillswap.skill_swap.model.wallet.SwapCoinWallet;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.persistence.EntityNotFoundException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import br.com.teamss.skillswap.skill_swap.model.config.WalletBalanceWebSocketHandler;
import br.com.teamss.skillswap.skill_swap.model.messaging.PaymentFulfillmentMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.AmqpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Owns all mutations of a customer's balance. The balance is an optimisation
 * for reads; the immutable ledger remains the financial audit trail.
 */
@Service
public class SwapCoinWalletService {

    private static final Logger log = LoggerFactory.getLogger(SwapCoinWalletService.class);

    private final UserRepository userRepository;
    private final SwapCoinWalletRepository walletRepository;
    private final SwapCoinLedgerRepository ledgerRepository;
    private final SwapCoinPaymentRepository paymentRepository;
    private final String stripeSecretKey;
    private final String stripeWebhookSecret;
    private final String frontendUrl;
    private final WalletBalanceWebSocketHandler balanceWebSocketHandler;
    private final RabbitTemplate rabbitTemplate;
    private final boolean rabbitEnabled;
    private final String rabbitExchange;
    private final String rabbitRoutingKey;
    private final boolean commerceEnabled;

    public SwapCoinWalletService(
        UserRepository userRepository,
        SwapCoinWalletRepository walletRepository,
        SwapCoinLedgerRepository ledgerRepository,
        SwapCoinPaymentRepository paymentRepository,
        @Value("${stripe.secret-key:}") String stripeSecretKey,
        @Value("${stripe.webhook-secret:}") String stripeWebhookSecret,
        @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
        WalletBalanceWebSocketHandler balanceWebSocketHandler,
        RabbitTemplate rabbitTemplate,
        @Value("${app.messaging.rabbitmq.enabled:true}") boolean rabbitEnabled,
        @Value("${app.messaging.rabbitmq.exchange:skillswap.payments}") String rabbitExchange,
        @Value("${app.messaging.rabbitmq.routing-key:payment.fulfillment}") String rabbitRoutingKey,
        @Value("${app.commerce.enabled:false}") boolean commerceEnabled
    ) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.ledgerRepository = ledgerRepository;
        this.paymentRepository = paymentRepository;
        this.stripeSecretKey = stripeSecretKey;
        this.stripeWebhookSecret = stripeWebhookSecret;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.balanceWebSocketHandler = balanceWebSocketHandler;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitEnabled = rabbitEnabled;
        this.rabbitExchange = rabbitExchange;
        this.rabbitRoutingKey = rabbitRoutingKey;
        this.commerceEnabled = commerceEnabled;
    }

    @Transactional
    public SwapCoinWalletResponse getWallet(UUID userId) {
        SwapCoinWallet wallet = getOrCreateWallet(userId);
        return new SwapCoinWalletResponse(wallet.getBalance(), wallet.getVersion());
    }

    public boolean isCommerceEnabled() {
        return commerceEnabled;
    }

    @Transactional(readOnly = true)
    public Page<SwapCoinLedgerResponse> getLedger(UUID userId, Pageable pageable) {
        return ledgerRepository.findByWallet_Owner_UserIdOrderByCreatedAtDesc(userId, pageable)
            .map(SwapCoinLedgerResponse::from);
    }

    @Transactional
    public CreateCheckoutSessionResponse createCheckoutSession(UUID userId, String packageCode) {
        if (!commerceEnabled) {
            throw new IllegalStateException("A compra de SwapCoins está desativada no modo gratuito do SkillSwap.");
        }
        ensureStripeApiKey();
        SwapCoinPackage selectedPackage = SwapCoinPackage.fromCode(packageCode);
        User user = getUser(userId);

        SwapCoinPayment payment = new SwapCoinPayment();
        payment.setUser(user);
        payment.setPackageCode(selectedPackage.getCode());
        payment.setCoinAmount(selectedPackage.getCoins());
        payment.setAmountCents(selectedPackage.getPriceCents());
        payment = paymentRepository.saveAndFlush(payment);

        try {
            Session session = Session.create(buildCheckoutParams(payment, selectedPackage), RequestOptions.builder()
                .setIdempotencyKey("swapcoin-checkout-" + payment.getPaymentId())
                .build());
            payment.setStripeCheckoutSessionId(session.getId());
            paymentRepository.save(payment);
            if (!StringUtils.hasText(session.getUrl())) {
                throw new IllegalStateException("A Stripe não retornou uma URL de checkout.");
            }
            return new CreateCheckoutSessionResponse(payment.getPaymentId(), session.getUrl());
        } catch (StripeException exception) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new IllegalStateException("Não foi possível iniciar o checkout da Stripe.", exception);
        }
    }

    /** Verifies Stripe's signature before any persistence action. */
    @Transactional
    public void handleStripeWebhook(String payload, String signature) {
        if (!commerceEnabled) return;
        if (!StringUtils.hasText(stripeWebhookSecret)) {
            throw new IllegalStateException("O segredo de webhook da Stripe não está configurado.");
        }

        final Event event;
        try {
            event = Webhook.constructEvent(payload, signature, stripeWebhookSecret);
        } catch (SignatureVerificationException exception) {
            throw new IllegalArgumentException("Assinatura de webhook da Stripe inválida.", exception);
        }

        if (!"checkout.session.completed".equals(event.getType())
            && !"checkout.session.async_payment_succeeded".equals(event.getType())) {
            return;
        }

        Optional<StripeObject> eventObject = event.getDataObjectDeserializer().getObject();
        if (eventObject.isEmpty() || !(eventObject.get() instanceof Session session)) {
            throw new IllegalArgumentException("Evento da Stripe sem sessão de checkout utilizável.");
        }
        if (session.getMetadata() == null || !session.getMetadata().containsKey("payment_id")) return;
        if (!"paid".equalsIgnoreCase(session.getPaymentStatus())) {
            return;
        }
        PaymentFulfillmentMessage message = new PaymentFulfillmentMessage(session.getId(), session.getAmountTotal(),
            session.getCurrency(), session.getPaymentIntent());
        if (rabbitEnabled) {
            try {
                rabbitTemplate.convertAndSend(rabbitExchange, rabbitRoutingKey, message);
            } catch (AmqpException brokerFailure) {
                log.error("RabbitMQ indisponivel; executando fulfillment Stripe idempotente em modo sincrono.", brokerFailure);
                fulfillPaidCheckoutValues(session.getId(), session.getAmountTotal(), session.getCurrency(), session.getPaymentIntent());
            }
        } else {
            fulfillPaidCheckoutValues(session.getId(), session.getAmountTotal(), session.getCurrency(), session.getPaymentIntent());
        }
    }

    @Transactional
    public SwapCoinWalletResponse transfer(UUID senderId, SwapCoinTransferRequest request) {
        if (senderId.equals(request.recipientUserId())) {
            throw new IllegalArgumentException("Não é possível transferir SwapCoins para a própria carteira.");
        }
        getUser(request.recipientUserId());

        UUID transferId = UUID.randomUUID();
        return transferLocked(senderId, request.recipientUserId(), request.amount(),
            StringUtils.hasText(request.note()) ? request.note().trim() : "Transferência entre usuários",
            "transfer:" + transferId, LedgerEntryType.TIP_SENT, LedgerEntryType.TIP_RECEIVED);
    }

    @Transactional
    public void debitForFeature(UUID userId, long amount, String description, String idempotencyKey) {
        if (amount <= 0) throw new IllegalArgumentException("O custo da funcionalidade deve ser positivo.");
        if (ledgerRepository.existsByIdempotencyKey(idempotencyKey)) return;
        SwapCoinWallet wallet = getOrCreateLockedWallet(userId);
        wallet.debit(amount);
        walletRepository.save(wallet);
        appendLedger(wallet, LedgerEntryType.FEATURE_SPEND, -amount, description, idempotencyKey);
        publishBalance(wallet);
    }

    @Transactional
    public void refundFeature(UUID userId, long amount, String description, String idempotencyKey) {
        if (amount <= 0 || ledgerRepository.existsByIdempotencyKey(idempotencyKey)) return;
        SwapCoinWallet wallet = getOrCreateLockedWallet(userId);
        wallet.credit(amount);
        walletRepository.save(wallet);
        appendLedger(wallet, LedgerEntryType.REFUND, amount, description, idempotencyKey);
        publishBalance(wallet);
    }

    @Transactional
    public void grantAchievementReward(UUID userId, long amount, String description, String idempotencyKey) {
        if (amount <= 0 || ledgerRepository.existsByIdempotencyKey(idempotencyKey)) return;
        SwapCoinWallet wallet = getOrCreateLockedWallet(userId);
        wallet.credit(amount);
        walletRepository.save(wallet);
        appendLedger(wallet, LedgerEntryType.ACHIEVEMENT_REWARD, amount, description, idempotencyKey);
        publishBalance(wallet);
    }

    @Transactional
    public SwapCoinWalletResponse adjustBalance(UUID userId, long amount, String reason, String idempotencyKey) {
        if (amount == 0) throw new IllegalArgumentException("O ajuste administrativo nao pode ser zero.");
        String key = "admin-adjust:" + userId + ":" + (StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : UUID.randomUUID());
        if (ledgerRepository.existsByIdempotencyKey(key)) return getWallet(userId);
        SwapCoinWallet wallet = getOrCreateLockedWallet(userId);
        if (amount > 0) {
            wallet.credit(amount);
        } else {
            wallet.debit(Math.abs(amount));
        }
        walletRepository.save(wallet);
        appendLedger(wallet, LedgerEntryType.ADJUSTMENT, amount, reason, key);
        publishBalance(wallet);
        return new SwapCoinWalletResponse(wallet.getBalance(), wallet.getVersion());
    }

    @Transactional
    public void payLessonExtra(UUID payerId, UUID teacherId, long amount, String description, String idempotencyKey) {
        if (payerId.equals(teacherId)) throw new IllegalArgumentException("O professor e o aluno devem ser diferentes.");
        if (amount <= 0 || ledgerRepository.existsByIdempotencyKey(idempotencyKey + ":debit")) return;
        getUser(teacherId);
        transferLocked(payerId, teacherId, amount, description, idempotencyKey,
            LedgerEntryType.FEATURE_SPEND, LedgerEntryType.LESSON_PAYMENT);
    }

    @Transactional
    public void fulfillPaidCheckout(String sessionId, Long amountTotal, String currency, String paymentIntent) {
        if (!commerceEnabled) return;
        fulfillPaidCheckoutValues(sessionId, amountTotal, currency, paymentIntent);
    }

    private SwapCoinWalletResponse transferLocked(UUID senderId, UUID recipientId, long amount, String note, String transferKey,
                                                  LedgerEntryType debitType, LedgerEntryType creditType) {
        if (amount <= 0) throw new IllegalArgumentException("A quantidade de SwapCoins deve ser positiva.");
        List<UUID> orderedUserIds = List.of(senderId, recipientId).stream()
            .sorted(Comparator.comparing(UUID::toString)).toList();
        SwapCoinWallet first = getOrCreateLockedWallet(orderedUserIds.get(0));
        SwapCoinWallet second = getOrCreateLockedWallet(orderedUserIds.get(1));
        SwapCoinWallet sender = senderId.equals(orderedUserIds.get(0)) ? first : second;
        SwapCoinWallet recipient = senderId.equals(orderedUserIds.get(0)) ? second : first;
        if (ledgerRepository.existsByIdempotencyKey(transferKey + ":debit")) {
            return new SwapCoinWalletResponse(sender.getBalance(), sender.getVersion());
        }
        sender.debit(amount);
        recipient.credit(amount);
        walletRepository.save(sender);
        walletRepository.save(recipient);
        appendLedger(sender, debitType, -amount, note, transferKey + ":debit");
        appendLedger(recipient, creditType, amount, note, transferKey + ":credit");
        publishBalance(sender);
        publishBalance(recipient);
        return new SwapCoinWalletResponse(sender.getBalance(), sender.getVersion());
    }

    private SessionCreateParams buildCheckoutParams(SwapCoinPayment payment, SwapCoinPackage selectedPackage) {
        SessionCreateParams.LineItem.PriceData.ProductData product = SessionCreateParams.LineItem.PriceData.ProductData.builder()
            .setName(selectedPackage.getCoins() + " SwapCoins — " + selectedPackage.getDisplayName())
            .setDescription("Créditos digitais de uso exclusivo na plataforma SkillSwap.")
            .build();
        SessionCreateParams.LineItem.PriceData price = SessionCreateParams.LineItem.PriceData.builder()
            .setCurrency("brl")
            .setUnitAmount(selectedPackage.getPriceCents())
            .setProductData(product)
            .build();
        SessionCreateParams.LineItem item = SessionCreateParams.LineItem.builder()
            .setQuantity(1L)
            .setPriceData(price)
            .build();

        return SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.PAYMENT)
            .setClientReferenceId(payment.getPaymentId().toString())
            .putMetadata("payment_id", payment.getPaymentId().toString())
            .putMetadata("package_code", selectedPackage.getCode())
            .setSuccessUrl(frontendUrl + "/financeiro?checkout=success&session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl(frontendUrl + "/financeiro?checkout=cancelled")
            .addLineItem(item)
            .build();
    }

    private void fulfillPaidCheckoutValues(String sessionId, Long amountTotal, String currency, String paymentIntent) {
        SwapCoinPayment payment = paymentRepository.findByStripeCheckoutSessionIdForUpdate(sessionId)
            .orElse(null);
        if (payment == null || payment.getStatus() == PaymentStatus.PAID) return;

        if (amountTotal == null || amountTotal.longValue() != payment.getAmountCents()
            || !"brl".equalsIgnoreCase(currency)) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new IllegalStateException("Valores da sessão Stripe não conferem com o pedido de SwapCoins.");
        }

        SwapCoinWallet wallet = getOrCreateLockedWallet(payment.getUser().getUserId());
        String idempotencyKey = "stripe:" + sessionId;
        if (!ledgerRepository.existsByIdempotencyKey(idempotencyKey)) {
            wallet.credit(payment.getCoinAmount());
            walletRepository.save(wallet);
            appendLedger(wallet, LedgerEntryType.PURCHASE, payment.getCoinAmount(),
                "Compra de " + payment.getCoinAmount() + " SwapCoins via Stripe", idempotencyKey);
        }
        payment.markPaid(paymentIntent);
        paymentRepository.save(payment);
        publishBalance(wallet);
    }

    private SwapCoinWallet getOrCreateWallet(UUID userId) {
        return walletRepository.findByOwner_UserId(userId).orElseGet(() -> createWallet(getUser(userId)));
    }

    private SwapCoinWallet getOrCreateLockedWallet(UUID userId) {
        return walletRepository.findByOwnerUserIdForUpdate(userId).orElseGet(() -> createWallet(getUser(userId)));
    }

    private SwapCoinWallet createWallet(User user) {
        SwapCoinWallet wallet = new SwapCoinWallet();
        wallet.setOwner(user);
        wallet = walletRepository.saveAndFlush(wallet);
        // The welcome grant is ledgered exactly once because a user can own one wallet.
        wallet.credit(15);
        walletRepository.save(wallet);
        appendLedger(wallet, LedgerEntryType.WELCOME_BONUS, 15, "Bônus de boas-vindas SkillSwap", "welcome:" + user.getUserId());
        return wallet;
    }

    private void appendLedger(SwapCoinWallet wallet, LedgerEntryType type, long amount, String description, String idempotencyKey) {
        if (ledgerRepository.existsByIdempotencyKey(idempotencyKey)) return;
        SwapCoinLedgerEntry entry = new SwapCoinLedgerEntry();
        entry.setWallet(wallet);
        entry.setEntryType(type);
        entry.setAmount(amount);
        entry.setBalanceAfter(wallet.getBalance());
        entry.setDescription(description);
        entry.setIdempotencyKey(idempotencyKey);
        ledgerRepository.save(entry);
    }

    private void publishBalance(SwapCoinWallet wallet) {
        if (balanceWebSocketHandler != null) {
            balanceWebSocketHandler.broadcast(wallet.getOwner().getUserId(),
                new SwapCoinWalletResponse(wallet.getBalance(), wallet.getVersion()));
        }
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
    }

    private void ensureStripeApiKey() {
        if (!StringUtils.hasText(stripeSecretKey) || !(stripeSecretKey.startsWith("sk_") || stripeSecretKey.startsWith("rk_"))) {
            throw new IllegalStateException("A chave secreta da Stripe não está configurada.");
        }
        Stripe.apiKey = stripeSecretKey;
    }
}
