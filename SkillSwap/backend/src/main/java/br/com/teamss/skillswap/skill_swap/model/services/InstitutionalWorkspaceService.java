package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.InstitutionalDto;
import br.com.teamss.skillswap.skill_swap.model.entities.AccessOpportunity;
import br.com.teamss.skillswap.skill_swap.model.entities.Organization;
import br.com.teamss.skillswap.skill_swap.model.entities.OrganizationMembership;
import br.com.teamss.skillswap.skill_swap.model.entities.OpportunityApplication;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.AccessOpportunityRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.GrowthPlanRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.OrganizationMembershipRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.OrganizationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.OpportunityApplicationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SkillEvidenceRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.text.Normalizer;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class InstitutionalWorkspaceService {
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of("BRL", "EUR", "USD");
    private static final Set<String> BRAZIL_COUNTRIES = Set.of("BR", "BRASIL", "BRAZIL");
    private static final Set<String> EUROPE_COUNTRIES = Set.of(
        "ALBANIA", "ANDORRA", "AUSTRIA", "BELGIUM", "BOSNIA", "BULGARIA", "CROATIA", "CYPRUS", "CZECHIA", "CZECH REPUBLIC",
        "DENMARK", "ESTONIA", "FINLAND", "FRANCE", "GERMANY", "GREECE", "HUNGARY", "ICELAND", "IRELAND", "ITALY", "KOSOVO",
        "LATVIA", "LIECHTENSTEIN", "LITHUANIA", "LUXEMBOURG", "MALTA", "MOLDOVA", "MONACO", "MONTENEGRO", "NETHERLANDS",
        "NORTH MACEDONIA", "NORWAY", "POLAND", "PORTUGAL", "ROMANIA", "SAN MARINO", "SERBIA", "SLOVAKIA", "SLOVENIA",
        "SPAIN", "SWEDEN", "SWITZERLAND", "TURKEY", "UKRAINE", "UNITED KINGDOM", "VATICAN", "VATICAN CITY", "EU", "EUROPE",
        "ALEMANHA", "BELGICA", "ESPANHA", "FRANCA", "GRECIA", "HOLANDA", "IRLANDA", "ITALIA", "NORUEGA", "POLONIA", "REINO UNIDO", "SUECIA", "SUICA"
    );
    private static final Set<String> MANAGEMENT_ROLES = Set.of("OWNER", "ADMIN", "MANAGER");
    private static final Set<String> BILLING_ROLES = Set.of("OWNER", "ADMIN");
    private static final Set<String> MEMBER_ROLES = Set.of("ADMIN", "MANAGER", "ANALYST", "MEMBER");

    private final OrganizationRepository organizationRepository;
    private final OrganizationMembershipRepository membershipRepository;
    private final AccessOpportunityRepository opportunityRepository;
    private final OpportunityApplicationRepository applicationRepository;
    private final GrowthPlanRepository planRepository;
    private final SkillEvidenceRepository evidenceRepository;
    private final UserRepository userRepository;
    private final UserServiceDTO userServiceDTO;
    private final String stripeSecretKey;
    private final boolean commerceEnabled;

    public InstitutionalWorkspaceService(
        OrganizationRepository organizationRepository,
        OrganizationMembershipRepository membershipRepository,
        AccessOpportunityRepository opportunityRepository,
        OpportunityApplicationRepository applicationRepository,
        GrowthPlanRepository planRepository,
        SkillEvidenceRepository evidenceRepository,
        UserRepository userRepository,
        UserServiceDTO userServiceDTO,
        @Value("${stripe.secret-key:}") String stripeSecretKey,
        @Value("${app.commerce.enabled:false}") boolean commerceEnabled
    ) {
        this.organizationRepository = organizationRepository;
        this.membershipRepository = membershipRepository;
        this.opportunityRepository = opportunityRepository;
        this.applicationRepository = applicationRepository;
        this.planRepository = planRepository;
        this.evidenceRepository = evidenceRepository;
        this.userRepository = userRepository;
        this.userServiceDTO = userServiceDTO;
        this.stripeSecretKey = stripeSecretKey;
        this.commerceEnabled = commerceEnabled;
    }

    @Transactional
    public InstitutionalDto.OrganizationResponse createOrganization(InstitutionalDto.OrganizationRequest request) {
        User user = currentUser();
        Organization organization = new Organization();
        organization.setName(required(request.name(), "Informe o nome da organização."));
        organization.setLegalName(trim(request.legalName()));
        organization.setType(request.type().trim().toUpperCase(Locale.ROOT));
        organization.setCountry(trim(request.country()));
        organization.setWebsite(trim(request.website()));
        organization.setBillingEmail(trim(request.billingEmail()));
        organization.setBillingCurrency(currencyForCountry(organization.getCountry()));
        organization.setMonthlyPriceCents(price(plan("TRIAL"), organization.getBillingCurrency()));
        organization.setCreatedBy(user);
        organization.setSlug(uniqueSlug(organization.getName()));
        organization = organizationRepository.saveAndFlush(organization);

        OrganizationMembership membership = new OrganizationMembership();
        membership.setOrganization(organization);
        membership.setUser(user);
        membership.setRole("OWNER");
        membership.setStatus("ACTIVE");
        membershipRepository.save(membership);
        return toOrganization(organization, membership);
    }

    @Transactional(readOnly = true)
    public List<InstitutionalDto.OrganizationResponse> organizations() {
        UUID userId = currentUser().getUserId();
        return membershipRepository.findByUser_UserIdAndStatusOrderByCreatedAtDesc(userId, "ACTIVE")
            .stream().map(membership -> toOrganization(membership.getOrganization(), membership)).toList();
    }

    @Transactional(readOnly = true)
    public InstitutionalDto.DashboardResponse dashboard(UUID organizationId) {
        OrganizationMembership membership = requireMembership(organizationId, Set.of("OWNER", "ADMIN", "MANAGER", "ANALYST", "MEMBER"));
        Organization organization = membership.getOrganization();
        List<OrganizationMembership> members = membershipRepository
            .findByOrganization_OrganizationIdAndStatusOrderByCreatedAtAsc(organizationId, "ACTIVE");
        List<UUID> userIds = members.stream().map(item -> item.getUser().getUserId()).toList();
        long opportunities = opportunityRepository.countByOrganization_OrganizationId(organizationId);
        long openOpportunities = opportunityRepository.countByOrganization_OrganizationIdAndStatus(organizationId, "OPEN");
        long applications = applicationRepository.countByOpportunity_Organization_OrganizationId(organizationId);
        Map<String, Long> byStatus = new LinkedHashMap<>();
        applicationRepository.findByOpportunity_Organization_OrganizationIdOrderByCreatedAtDesc(organizationId)
            .forEach(application -> byStatus.merge(application.getStatus(), 1L, Long::sum));
        List<InstitutionalDto.OpportunityResponse> recent = opportunityRepository
            .findByOrganization_OrganizationIdOrderByCreatedAtDesc(organizationId).stream().limit(8)
            .map(this::toOpportunity).toList();
        return new InstitutionalDto.DashboardResponse(
            toOrganization(organization, membership), members.size(),
            membershipRepository.countByOrganization_OrganizationIdAndStatus(organizationId, "ACTIVE"),
            opportunities, openOpportunities, applications,
            userIds.isEmpty() ? 0 : planRepository.countByOwner_UserIdIn(userIds),
            userIds.isEmpty() ? 0 : evidenceRepository.countByOwner_UserIdIn(userIds),
            byStatus, recent
        );
    }

    @Transactional(readOnly = true)
    public List<InstitutionalDto.MemberResponse> members(UUID organizationId) {
        requireMembership(organizationId, Set.of("OWNER", "ADMIN", "MANAGER", "ANALYST", "MEMBER"));
        return membershipRepository.findByOrganization_OrganizationIdAndStatusOrderByCreatedAtAsc(organizationId, "ACTIVE")
            .stream().map(this::toMember).toList();
    }

    @Transactional
    public InstitutionalDto.MemberResponse addMember(UUID organizationId, InstitutionalDto.MemberRequest request) {
        OrganizationMembership actor = requireMembership(organizationId, MANAGEMENT_ROLES);
        Organization organization = actor.getOrganization();
        ensureSeatAvailable(organization);
        String role = StringUtils.hasText(request.role()) ? request.role().trim().toUpperCase(Locale.ROOT) : "MEMBER";
        if (!MEMBER_ROLES.contains(role)) throw new IllegalArgumentException("Papel institucional inválido.");
        User user = userRepository.findByEmail(request.email().trim().toLowerCase(Locale.ROOT))
            .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado. A pessoa deve criar uma conta antes do vínculo institucional."));
        OrganizationMembership membership = membershipRepository.findByOrganization_OrganizationIdAndUser_UserId(organizationId, user.getUserId())
            .orElseGet(OrganizationMembership::new);
        membership.setOrganization(organization);
        membership.setUser(user);
        membership.setRole(role);
        membership.setStatus("ACTIVE");
        membership.setDepartment(trim(request.department()));
        return toMember(membershipRepository.save(membership));
    }

    @Transactional
    public InstitutionalDto.MemberResponse updateMember(UUID organizationId, UUID membershipId, InstitutionalDto.MemberUpdateRequest request) {
        OrganizationMembership actor = requireMembership(organizationId, MANAGEMENT_ROLES);
        OrganizationMembership member = membershipRepository.findByMembershipIdAndOrganization_OrganizationId(membershipId, organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Membro institucional não encontrado."));
        if ("OWNER".equals(member.getRole()) && (request.role() == null || !"OWNER".equalsIgnoreCase(request.role()))) {
            throw new IllegalArgumentException("O proprietário não pode ser removido sem transferir a organização.");
        }
        if (StringUtils.hasText(request.role())) {
            String role = request.role().trim().toUpperCase(Locale.ROOT);
            if (!MEMBER_ROLES.contains(role) && !"OWNER".equals(role)) throw new IllegalArgumentException("Papel institucional inválido.");
            if ("OWNER".equals(role) && !"OWNER".equals(actor.getRole())) throw new IllegalArgumentException("Somente o proprietário pode transferir a propriedade.");
            member.setRole(role);
        }
        if (StringUtils.hasText(request.status())) member.setStatus(request.status().trim().toUpperCase(Locale.ROOT));
        if (request.department() != null) member.setDepartment(trim(request.department()));
        member.touch();
        return toMember(membershipRepository.save(member));
    }

    @Transactional
    public void removeMember(UUID organizationId, UUID membershipId) {
        OrganizationMembership actor = requireMembership(organizationId, MANAGEMENT_ROLES);
        OrganizationMembership member = membershipRepository.findByMembershipIdAndOrganization_OrganizationId(membershipId, organizationId)
            .orElseThrow(() -> new EntityNotFoundException("Membro institucional não encontrado."));
        if ("OWNER".equals(member.getRole())) throw new IllegalArgumentException("O proprietário não pode ser removido.");
        if (actor.getMembershipId().equals(member.getMembershipId()) && "OWNER".equals(actor.getRole())) {
            throw new IllegalArgumentException("O proprietário não pode sair sem transferir a organização.");
        }
        member.setStatus("SUSPENDED");
        member.touch();
        membershipRepository.save(member);
    }

    @Transactional(readOnly = true)
    public List<InstitutionalDto.OpportunityResponse> opportunities(UUID organizationId) {
        requireMembership(organizationId, Set.of("OWNER", "ADMIN", "MANAGER", "ANALYST", "MEMBER"));
        return opportunityRepository.findByOrganization_OrganizationIdOrderByCreatedAtDesc(organizationId)
            .stream().map(this::toOpportunity).toList();
    }

    @Transactional
    public InstitutionalDto.OpportunityResponse createOpportunity(UUID organizationId, InstitutionalDto.OpportunityRequest request) {
        OrganizationMembership membership = requireMembership(organizationId, MANAGEMENT_ROLES);
        AccessOpportunity opportunity = new AccessOpportunity();
        opportunity.setOrganization(membership.getOrganization());
        opportunity.setOrganizationName(membership.getOrganization().getName());
        opportunity.setCreatedBy(currentUser());
        opportunity.setTitle(required(request.title(), "Informe o título da oportunidade."));
        opportunity.setDescription(required(request.description(), "Informe a descrição da oportunidade."));
        opportunity.setRequiredSkills(required(request.requiredSkills(), "Informe as habilidades necessárias."));
        opportunity.setRemote(request.remote());
        opportunity.setAccessibleSupport(request.accessibleSupport());
        opportunity.setLocation(trim(request.location()));
        opportunity.setStatus(StringUtils.hasText(request.status()) ? request.status().trim().toUpperCase(Locale.ROOT) : "OPEN");
        return toOpportunity(opportunityRepository.save(opportunity));
    }

    @Transactional
    public InstitutionalDto.OpportunityResponse updateOpportunityStatus(UUID organizationId, UUID opportunityId, String status) {
        requireMembership(organizationId, MANAGEMENT_ROLES);
        AccessOpportunity opportunity = opportunityRepository.findById(opportunityId)
            .filter(item -> item.getOrganization() != null && organizationId.equals(item.getOrganization().getOrganizationId()))
            .orElseThrow(() -> new EntityNotFoundException("Oportunidade institucional não encontrada."));
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("DRAFT", "OPEN", "CLOSED", "ARCHIVED").contains(normalized)) throw new IllegalArgumentException("Status de oportunidade inválido.");
        opportunity.setStatus(normalized);
        return toOpportunity(opportunityRepository.save(opportunity));
    }

    @Transactional(readOnly = true)
    public List<InstitutionalDto.ApplicationResponse> applications(UUID organizationId) {
        requireMembership(organizationId, Set.of("OWNER", "ADMIN", "MANAGER", "ANALYST"));
        return applicationRepository.findByOpportunity_Organization_OrganizationIdOrderByCreatedAtDesc(organizationId)
            .stream().map(this::toApplication).toList();
    }

    @Transactional
    public InstitutionalDto.ApplicationResponse updateApplicationStatus(UUID organizationId, UUID applicationId, String status) {
        requireMembership(organizationId, MANAGEMENT_ROLES);
        OpportunityApplication application = applicationRepository.findById(applicationId)
            .filter(item -> item.getOpportunity().getOrganization() != null && organizationId.equals(item.getOpportunity().getOrganization().getOrganizationId()))
            .orElseThrow(() -> new EntityNotFoundException("Candidatura institucional não encontrada."));
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("SUBMITTED", "REVIEWING", "ACCEPTED", "REJECTED", "WITHDRAWN").contains(normalized)) throw new IllegalArgumentException("Status de candidatura inválido.");
        application.setStatus(normalized);
        return toApplication(applicationRepository.save(application));
    }

    @Transactional
    public InstitutionalDto.SubscriptionResponse updateSubscription(UUID organizationId, InstitutionalDto.SubscriptionRequest request) {
        OrganizationMembership membership = requireMembership(organizationId, BILLING_ROLES);
        Organization organization = membership.getOrganization();
        PlanDefinition plan = plan(request.planCode());
        int seats = request.seatLimit() == null ? plan.defaultSeats() : request.seatLimit();
        if (seats < membershipRepository.countByOrganization_OrganizationIdAndStatus(organizationId, "ACTIVE")) {
            throw new IllegalArgumentException("O limite de assentos não pode ser menor que os membros ativos.");
        }
        organization.setPlanCode(plan.code());
        organization.setSeatLimit(seats);
        organization.setBillingCurrency(currency(request.currency(), organization.getBillingCurrency()));
        organization.setMonthlyPriceCents(price(plan, organization.getBillingCurrency()));
        if (StringUtils.hasText(request.billingEmail())) organization.setBillingEmail(request.billingEmail().trim().toLowerCase(Locale.ROOT));
        if ("TRIAL".equals(plan.code())) organization.setSubscriptionStatus("TRIAL");
        else if (!"ACTIVE".equals(organization.getSubscriptionStatus())) organization.setSubscriptionStatus("MANUAL");
        organization.touch();
        return toSubscription(organization);
    }

    @Transactional(readOnly = true)
    public InstitutionalDto.SubscriptionResponse subscription(UUID organizationId) {
        requireMembership(organizationId, Set.of("OWNER", "ADMIN", "MANAGER", "ANALYST", "MEMBER"));
        return toSubscription(organizationRepository.findById(organizationId).orElseThrow());
    }

    public OrganizationMembership requireMembership(UUID organizationId, Set<String> allowedRoles) {
        if (organizationId == null) throw new IllegalArgumentException("Organização obrigatória.");
        OrganizationMembership membership = membershipRepository.findByOrganization_OrganizationIdAndUser_UserId(organizationId, currentUser().getUserId())
            .orElseThrow(() -> new org.springframework.security.access.AccessDeniedException("Você não pertence a esta organização."));
        if (!"ACTIVE".equals(membership.getStatus()) || !allowedRoles.contains(membership.getRole().toUpperCase(Locale.ROOT))) {
            throw new org.springframework.security.access.AccessDeniedException("Você não possui permissão para esta operação institucional.");
        }
        return membership;
    }

    public User currentUser() {
        UUID userId = userServiceDTO.getAuthenticatedUser().getUserId();
        return userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
    }

    public long activeMemberCount(UUID organizationId) {
        return membershipRepository.countByOrganization_OrganizationIdAndStatus(organizationId, "ACTIVE");
    }

    public static List<InstitutionalDto.PlanCatalogItem> catalog() {
        return List.of(
            new InstitutionalDto.PlanCatalogItem("TRIAL", "Piloto", 5, 0, 0, 0, "Para validação inicial com uma equipe pequena."),
            new InstitutionalDto.PlanCatalogItem("TEAM", "Team", 25, 4900, 4500, 24900, "Workspace institucional para uma equipe em operação."),
            new InstitutionalDto.PlanCatalogItem("GROWTH", "Growth", 100, 19900, 18500, 99900, "Relatórios, oportunidades e programas em escala."),
            new InstitutionalDto.PlanCatalogItem("ENTERPRISE", "Enterprise", 500, 79900, 74900, 399900, "Contrato corporativo com implantação e suporte dedicado.")
        );
    }

    public static PlanDefinition plan(String code) {
        String normalized = code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "TRIAL" -> new PlanDefinition("TRIAL", 5, 0, 0, 0);
            case "TEAM" -> new PlanDefinition("TEAM", 25, 4900, 4500, 24900);
            case "GROWTH" -> new PlanDefinition("GROWTH", 100, 19900, 18500, 99900);
            case "ENTERPRISE" -> new PlanDefinition("ENTERPRISE", 500, 79900, 74900, 399900);
            default -> throw new IllegalArgumentException("Plano institucional inválido.");
        };
    }

    public record PlanDefinition(String code, int defaultSeats, long usdCents, long eurCents, long brlCents) {}

    public static long price(PlanDefinition plan, String currency) {
        return switch (normalizeCurrency(currency, "USD")) {
            case "BRL" -> plan.brlCents();
            case "EUR" -> plan.eurCents();
            default -> plan.usdCents();
        };
    }

    public static String normalizeCurrency(String requested, String fallback) {
        String value = StringUtils.hasText(requested) ? requested.trim().toUpperCase(Locale.ROOT) : fallback;
        if (!StringUtils.hasText(value)) value = "USD";
        value = value.trim().toUpperCase(Locale.ROOT);
        if (!SUPPORTED_CURRENCIES.contains(value)) throw new IllegalArgumentException("A cobrança institucional usa BRL, EUR ou USD.");
        return value;
    }

    public static String currencyForCountry(String country) {
        if (!StringUtils.hasText(country)) return "USD";
        String normalized = Normalizer.normalize(country.trim().toUpperCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        if (BRAZIL_COUNTRIES.contains(normalized)) return "BRL";
        if (EUROPE_COUNTRIES.contains(normalized)) return "EUR";
        return "USD";
    }

    private void ensureSeatAvailable(Organization organization) {
        long active = membershipRepository.countByOrganization_OrganizationIdAndStatus(organization.getOrganizationId(), "ACTIVE");
        if (active >= organization.getSeatLimit()) throw new IllegalArgumentException("O limite de assentos do plano institucional foi atingido.");
    }

    private InstitutionalDto.OrganizationResponse toOrganization(Organization organization, OrganizationMembership membership) {
        return new InstitutionalDto.OrganizationResponse(organization.getOrganizationId(), organization.getSlug(), organization.getName(), organization.getLegalName(), organization.getType(), organization.getCountry(), organization.getWebsite(), organization.getBillingEmail(), organization.getStatus(), organization.getPlanCode(), organization.getSubscriptionStatus(), organization.getSeatLimit(), membershipRepository.countByOrganization_OrganizationIdAndStatus(organization.getOrganizationId(), "ACTIVE"), organization.getBillingCurrency(), organization.getMonthlyPriceCents(), membership == null ? null : membership.getRole());
    }

    private InstitutionalDto.MemberResponse toMember(OrganizationMembership membership) {
        User user = membership.getUser();
        return new InstitutionalDto.MemberResponse(membership.getMembershipId(), user.getUserId(), user.getName(), user.getUsername(), user.getEmail(), membership.getRole(), membership.getStatus(), membership.getDepartment(), membership.getCreatedAt());
    }

    private InstitutionalDto.OpportunityResponse toOpportunity(AccessOpportunity opportunity) {
        long applications = opportunity.getOpportunityId() == null ? 0 : applicationRepository.countByOpportunity_OpportunityId(opportunity.getOpportunityId());
        return new InstitutionalDto.OpportunityResponse(opportunity.getOpportunityId(), opportunity.getOrganization() == null ? null : opportunity.getOrganization().getOrganizationId(), opportunity.getOrganizationName(), opportunity.getTitle(), opportunity.getDescription(), opportunity.getRequiredSkills(), opportunity.isRemote(), opportunity.isAccessibleSupport(), opportunity.getLocation(), opportunity.getStatus(), opportunity.getCreatedAt(), applications);
    }

    private InstitutionalDto.ApplicationResponse toApplication(OpportunityApplication application) {
        User applicant = application.getApplicant();
        return new InstitutionalDto.ApplicationResponse(application.getApplicationId(), application.getOpportunity().getOpportunityId(), application.getOpportunity().getTitle(), applicant.getUserId(), applicant.getName(), applicant.getUsername(), applicant.getEmail(), application.getStatus(), application.getMessage(), application.getCreatedAt(), application.getUpdatedAt());
    }

    private InstitutionalDto.SubscriptionResponse toSubscription(Organization organization) {
        return new InstitutionalDto.SubscriptionResponse(organization.getPlanCode(), organization.getSubscriptionStatus(), organization.getSeatLimit(), organization.getBillingCurrency(), organization.getMonthlyPriceCents(), organization.getBillingEmail(), commerceEnabled && StringUtils.hasText(stripeSecretKey), organization.getStripeCheckoutSessionId());
    }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}", "").replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
        if (base.isBlank()) base = "organization";
        String slug = base;
        int suffix = 2;
        while (organizationRepository.findBySlugIgnoreCase(slug).isPresent()) slug = base + "-" + suffix++;
        return slug.substring(0, Math.min(slug.length(), 120));
    }

    private static String currency(String requested, String current) { return normalizeCurrency(requested, StringUtils.hasText(current) ? current : "USD"); }
    private static String required(String value, String message) { if (!StringUtils.hasText(value)) throw new IllegalArgumentException(message); return value.trim(); }
    private static String trim(String value) { return StringUtils.hasText(value) ? value.trim() : null; }
}
