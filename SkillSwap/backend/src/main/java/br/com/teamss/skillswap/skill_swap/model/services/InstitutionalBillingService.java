package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.InstitutionalDto;
import br.com.teamss.skillswap.skill_swap.model.entities.Organization;
import br.com.teamss.skillswap.skill_swap.model.entities.OrganizationMembership;
import br.com.teamss.skillswap.skill_swap.model.repositories.OrganizationRepository;
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
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InstitutionalBillingService {
    private final InstitutionalWorkspaceService workspaceService;
    private final OrganizationRepository organizationRepository;
    private final String stripeSecretKey;
    private final String stripeWebhookSecret;
    private final String frontendUrl;
    private final boolean commerceEnabled;

    public InstitutionalBillingService(
        InstitutionalWorkspaceService workspaceService,
        OrganizationRepository organizationRepository,
        @Value("${stripe.secret-key:}") String stripeSecretKey,
        @Value("${stripe.webhook-secret:}") String stripeWebhookSecret,
        @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
        @Value("${app.commerce.enabled:false}") boolean commerceEnabled
    ) {
        this.workspaceService = workspaceService;
        this.organizationRepository = organizationRepository;
        this.stripeSecretKey = stripeSecretKey;
        this.stripeWebhookSecret = stripeWebhookSecret;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.commerceEnabled = commerceEnabled;
    }

    @Transactional
    public InstitutionalDto.CheckoutResponse createCheckout(UUID organizationId, InstitutionalDto.SubscriptionRequest request) {
        if (!commerceEnabled) {
            throw new IllegalStateException("O checkout institucional está desativado no modo gratuito do SkillSwap.");
        }
        OrganizationMembership membership = workspaceService.requireMembership(organizationId, Set.of("OWNER", "ADMIN"));
        ensureStripeKey();
        InstitutionalWorkspaceService.PlanDefinition plan = InstitutionalWorkspaceService.plan(request.planCode());
        String currency = InstitutionalWorkspaceService.normalizeCurrency(request.currency(), membership.getOrganization().getBillingCurrency());
        int seats = request.seatLimit() == null ? plan.defaultSeats() : request.seatLimit();
        if (seats < workspaceService.activeMemberCount(organizationId)) throw new IllegalArgumentException("O limite de assentos não pode ser menor que os membros ativos.");
        long amount = InstitutionalWorkspaceService.price(plan, currency);
        if (amount <= 0) throw new IllegalArgumentException("O plano piloto não precisa de checkout.");

        Organization organization = membership.getOrganization();
        SessionCreateParams.LineItem.PriceData.ProductData product = SessionCreateParams.LineItem.PriceData.ProductData.builder()
            .setName("SkillSwap Institutional - " + plan.code())
            .setDescription("Workspace institucional SkillSwap para " + seats + " assentos.")
            .build();
        SessionCreateParams.LineItem.PriceData.Recurring recurring = SessionCreateParams.LineItem.PriceData.Recurring.builder()
            .setInterval(SessionCreateParams.LineItem.PriceData.Recurring.Interval.MONTH)
            .build();
        SessionCreateParams.LineItem.PriceData price = SessionCreateParams.LineItem.PriceData.builder()
            .setCurrency(currency.toLowerCase(Locale.ROOT))
            .setUnitAmount(amount)
            .setProductData(product)
            .setRecurring(recurring)
            .build();
        SessionCreateParams.LineItem item = SessionCreateParams.LineItem.builder().setQuantity(1L).setPriceData(price).build();
        SessionCreateParams.Builder builder = SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
            .setClientReferenceId(organizationId.toString())
            .putMetadata("organization_id", organizationId.toString())
            .putMetadata("plan_code", plan.code())
            .putMetadata("seat_limit", String.valueOf(seats))
            .putMetadata("currency", currency)
            .setSuccessUrl(frontendUrl + "/institucional?billing=success&session_id={CHECKOUT_SESSION_ID}")
            .setCancelUrl(frontendUrl + "/institucional?billing=cancelled")
            .addLineItem(item);
        if (StringUtils.hasText(request.billingEmail())) builder.setCustomerEmail(request.billingEmail().trim().toLowerCase(Locale.ROOT));
        try {
            Session session = Session.create(builder.build(), RequestOptions.builder().setIdempotencyKey("institutional-checkout-" + organizationId + "-" + plan.code()).build());
            organization.setPlanCode(plan.code());
            organization.setSeatLimit(seats);
            organization.setBillingCurrency(currency);
            organization.setMonthlyPriceCents(amount);
            if (StringUtils.hasText(request.billingEmail())) organization.setBillingEmail(request.billingEmail().trim().toLowerCase(Locale.ROOT));
            organization.setSubscriptionStatus("PENDING");
            organization.setStripeCheckoutSessionId(session.getId());
            organization.touch();
            organizationRepository.save(organization);
            if (!StringUtils.hasText(session.getUrl())) throw new IllegalStateException("A Stripe não retornou uma URL de checkout.");
            return new InstitutionalDto.CheckoutResponse(organizationId.toString(), plan.code(), currency, session.getUrl(), session.getId());
        } catch (StripeException exception) {
            throw new IllegalStateException("Não foi possível iniciar o checkout institucional da Stripe.", exception);
        }
    }

    @Transactional
    public void handleWebhook(String payload, String signature) {
        if (!commerceEnabled) return;
        if (!StringUtils.hasText(stripeWebhookSecret)) throw new IllegalStateException("O segredo de webhook da Stripe não está configurado.");
        final Event event;
        try {
            event = Webhook.constructEvent(payload, signature, stripeWebhookSecret);
        } catch (SignatureVerificationException exception) {
            throw new IllegalArgumentException("Assinatura de webhook da Stripe inválida.", exception);
        }
        if (!"checkout.session.completed".equals(event.getType()) && !"checkout.session.async_payment_succeeded".equals(event.getType())) return;
        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        if (!(object instanceof Session session)) return;
        String organizationId = session.getMetadata() == null ? null : session.getMetadata().get("organization_id");
        if (!StringUtils.hasText(organizationId) || !"paid".equalsIgnoreCase(session.getPaymentStatus())) return;
        Organization organization = organizationRepository.findById(UUID.fromString(organizationId))
            .orElseThrow(() -> new EntityNotFoundException("Organização da cobrança não encontrada."));
        String planCode = session.getMetadata().get("plan_code");
        InstitutionalWorkspaceService.PlanDefinition plan = InstitutionalWorkspaceService.plan(planCode);
        int seats = Integer.parseInt(session.getMetadata().getOrDefault("seat_limit", String.valueOf(plan.defaultSeats())));
        organization.setPlanCode(plan.code());
        organization.setSeatLimit(seats);
        String currency = InstitutionalWorkspaceService.normalizeCurrency(session.getMetadata().get("currency"), "USD");
        organization.setBillingCurrency(currency);
        organization.setMonthlyPriceCents(InstitutionalWorkspaceService.price(plan, currency));
        organization.setSubscriptionStatus("ACTIVE");
        organization.setStripeCheckoutSessionId(session.getId());
        organization.setStripeCustomerId(session.getCustomer());
        organization.setStripeSubscriptionId(session.getSubscription());
        organization.touch();
        organizationRepository.save(organization);
    }

    private void ensureStripeKey() {
        if (!StringUtils.hasText(stripeSecretKey) || !(stripeSecretKey.startsWith("sk_") || stripeSecretKey.startsWith("rk_"))) throw new IllegalStateException("A chave secreta da Stripe não está configurada.");
        Stripe.apiKey = stripeSecretKey;
    }
}
