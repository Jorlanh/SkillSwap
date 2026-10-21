package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.OpportunityOsDto;
import br.com.teamss.skillswap.skill_swap.model.entities.AccessOpportunity;
import br.com.teamss.skillswap.skill_swap.model.entities.GrowthPlan;
import br.com.teamss.skillswap.skill_swap.model.entities.GrowthPlanStep;
import br.com.teamss.skillswap.skill_swap.model.entities.OpportunityApplication;
import br.com.teamss.skillswap.skill_swap.model.entities.SkillEvidence;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.AccessOpportunityRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.GrowthPlanRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.GrowthPlanStepRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.OpportunityApplicationRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SkillEvidenceRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityOsService {
    private final GrowthPlanRepository planRepository;
    private final GrowthPlanStepRepository stepRepository;
    private final SkillEvidenceRepository evidenceRepository;
    private final OpportunityApplicationRepository applicationRepository;
    private final AccessOpportunityRepository opportunityRepository;
    private final UserRepository userRepository;
    private final UserServiceDTO userServiceDTO;

    public OpportunityOsService(GrowthPlanRepository planRepository,
                                GrowthPlanStepRepository stepRepository,
                                SkillEvidenceRepository evidenceRepository,
                                OpportunityApplicationRepository applicationRepository,
                                AccessOpportunityRepository opportunityRepository,
                                UserRepository userRepository,
                                UserServiceDTO userServiceDTO) {
        this.planRepository = planRepository;
        this.stepRepository = stepRepository;
        this.evidenceRepository = evidenceRepository;
        this.applicationRepository = applicationRepository;
        this.opportunityRepository = opportunityRepository;
        this.userRepository = userRepository;
        this.userServiceDTO = userServiceDTO;
    }

    @Transactional(readOnly = true)
    public OpportunityOsDto.DashboardResponse dashboard() {
        User user = currentUser();
        GrowthPlan plan = activePlan(user.getUserId());
        long evidenceCount = evidenceRepository.countByOwner_UserId(user.getUserId());
        long applicationCount = applicationRepository.countByApplicant_UserId(user.getUserId());
        return new OpportunityOsDto.DashboardResponse(toPlan(plan), evidenceCount, applicationCount,
            nextAction(plan, evidenceCount, applicationCount));
    }

    @Transactional
    public OpportunityOsDto.PlanResponse createPlan(OpportunityOsDto.PlanRequest request) {
        if (request == null) throw new IllegalArgumentException("Informe os dados do plano.");
        String goal = required(request.goal(), "Informe o objetivo que deseja alcançar.");
        User user = currentUser();
        planRepository.findFirstByOwner_UserIdAndStatusOrderByUpdatedAtDesc(user.getUserId(), "ACTIVE")
            .ifPresent(previous -> {
                previous.setStatus("ARCHIVED");
                previous.touch();
                planRepository.save(previous);
            });

        GrowthPlan plan = new GrowthPlan();
        plan.setOwner(user);
        plan.setGoal(goal);
        plan.setTargetRole(trim(request.targetRole()));
        plan.setConstraints(trim(request.constraints()));
        plan.setWeeklyHours(clampHours(request.weeklyHours()));
        plan.setStatus("ACTIVE");
        plan.setProgressPercent(0);

        String focus = skillFocus(user);
        addStep(plan, 1, "Definir o resultado", "Escreva como você saberá que avançou e qual problema deseja resolver com essa habilidade.", "GOAL", focus, 1);
        addStep(plan, 2, "Aprender o essencial", "Escolha uma atividade curta e prática para dominar os fundamentos que sustentam seu objetivo.", "LEARNING", focus, Math.max(1, Math.min(8, plan.getWeeklyHours())));
        addStep(plan, 3, "Construir um projeto real", "Aplique o conhecimento em um desafio com uma entrega que outra pessoa possa testar, usar ou avaliar.", "PROJECT", focus, Math.max(2, Math.min(12, plan.getWeeklyHours() * 2)));
        addStep(plan, 4, "Publicar uma evidência", "Registre o que foi feito, o resultado, o link do artefato e peça uma revisão para transformar esforço em prova.", "EVIDENCE", focus, 2);

        return toPlan(planRepository.save(plan));
    }

    @Transactional
    public OpportunityOsDto.PlanResponse updateStep(Long stepId, OpportunityOsDto.StepUpdateRequest request) {
        if (stepId == null || request == null || !List.of("TODO", "IN_PROGRESS", "DONE").contains(request.status())) {
            throw new IllegalArgumentException("Status de etapa inválido.");
        }
        User user = currentUser();
        GrowthPlanStep step = stepRepository.findByStepIdAndPlan_Owner_UserId(stepId, user.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("Etapa não encontrada."));
        step.setStatus(request.status());
        GrowthPlan plan = step.getPlan();
        recalculate(plan);
        stepRepository.save(step);
        planRepository.save(plan);
        return toPlan(plan);
    }

    @Transactional(readOnly = true)
    public List<OpportunityOsDto.EvidenceResponse> evidences() {
        UUID userId = currentUser().getUserId();
        return evidenceRepository.findByOwner_UserIdOrderByCreatedAtDesc(userId).stream().map(this::toEvidence).toList();
    }

    @Transactional
    public OpportunityOsDto.EvidenceResponse createEvidence(OpportunityOsDto.EvidenceRequest request) {
        if (request == null) throw new IllegalArgumentException("Evidência inválida.");
        User user = currentUser();
        SkillEvidence evidence = new SkillEvidence();
        evidence.setOwner(user);
        evidence.setTitle(required(request.title(), "Informe um título para a evidência."));
        evidence.setSkillFocus(required(request.skillFocus(), "Informe a habilidade demonstrada."));
        evidence.setDescription(required(request.description(), "Descreva o que foi feito e qual foi o resultado."));
        evidence.setArtifactUrl(trim(request.artifactUrl()));
        evidence.setEvidenceType(isBlank(request.evidenceType()) ? "PROJECT" : request.evidenceType().trim().toUpperCase(Locale.ROOT));

        if (request.opportunityId() != null) {
            evidence.setOpportunity(opportunityRepository.findById(request.opportunityId())
                .orElseThrow(() -> new EntityNotFoundException("Oportunidade não encontrada.")));
        }
        if (request.planStepId() != null) {
            GrowthPlanStep step = stepRepository.findByStepIdAndPlan_Owner_UserId(request.planStepId(), user.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Etapa não encontrada."));
            evidence.setPlanStepId(step.getStepId());
            step.setStatus("DONE");
            stepRepository.save(step);
        }

        SkillEvidence saved = evidenceRepository.save(evidence);
        if (request.planStepId() != null) {
            GrowthPlanStep step = stepRepository.findById(request.planStepId()).orElseThrow();
            step.setEvidenceId(saved.getEvidenceId());
            recalculate(step.getPlan());
            stepRepository.save(step);
            planRepository.save(step.getPlan());
        }
        return toEvidence(saved);
    }

    @Transactional
    public OpportunityOsDto.EvidenceResponse requestEvidenceReview(UUID evidenceId) {
        SkillEvidence evidence = evidenceRepository.findByEvidenceIdAndOwner_UserId(evidenceId, currentUser().getUserId())
            .orElseThrow(() -> new EntityNotFoundException("Evidência não encontrada."));
        evidence.requestReview();
        return toEvidence(evidenceRepository.save(evidence));
    }

    @Transactional
    public OpportunityOsDto.ApplicationResponse apply(UUID opportunityId, OpportunityOsDto.ApplicationRequest request) {
        User user = currentUser();
        AccessOpportunity opportunity = opportunityRepository.findById(opportunityId)
            .orElseThrow(() -> new EntityNotFoundException("Oportunidade não encontrada."));
        if (!"OPEN".equalsIgnoreCase(opportunity.getStatus())) {
            throw new IllegalArgumentException("Esta oportunidade não está aberta para candidaturas.");
        }
        if (applicationRepository.existsByApplicant_UserIdAndOpportunity_OpportunityId(user.getUserId(), opportunityId)) {
            throw new IllegalArgumentException("Você já se candidatou a esta oportunidade.");
        }
        OpportunityApplication application = new OpportunityApplication();
        application.setApplicant(user);
        application.setOpportunity(opportunity);
        application.setMessage(trim(request == null ? null : request.message()));
        return toApplication(applicationRepository.save(application));
    }

    @Transactional(readOnly = true)
    public List<OpportunityOsDto.ApplicationResponse> applications() {
        return applicationRepository.findByApplicant_UserIdOrderByCreatedAtDesc(currentUser().getUserId())
            .stream().map(this::toApplication).toList();
    }

    private User currentUser() {
        UUID userId = userServiceDTO.getAuthenticatedUser().getUserId();
        return userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("Usuário autenticado não encontrado."));
    }

    private GrowthPlan activePlan(UUID userId) {
        return planRepository.findFirstByOwner_UserIdOrderByUpdatedAtDesc(userId).orElse(null);
    }

    private void addStep(GrowthPlan plan, int order, String title, String description, String kind, String focus, int hours) {
        GrowthPlanStep step = new GrowthPlanStep();
        step.setPlan(plan);
        step.setStepOrder(order);
        step.setTitle(title);
        step.setDescription(description);
        step.setKind(kind);
        step.setSkillFocus(focus);
        step.setEstimatedHours(hours);
        plan.getSteps().add(step);
    }

    private void recalculate(GrowthPlan plan) {
        int total = plan.getSteps().size();
        long done = plan.getSteps().stream().filter(step -> "DONE".equals(step.getStatus())).count();
        int progress = total == 0 ? 0 : (int) Math.round(done * 100.0 / total);
        plan.setProgressPercent(progress);
        plan.setStatus(progress == 100 ? "COMPLETED" : "ACTIVE");
        plan.touch();
    }

    private String skillFocus(User user) {
        if (user.getSkills() == null || user.getSkills().isEmpty()) return "habilidade alinhada ao objetivo";
        return user.getSkills().stream().limit(3).map(skill -> skill.getName()).collect(Collectors.joining(", "));
    }

    private String nextAction(GrowthPlan plan, long evidenceCount, long applications) {
        if (plan == null) return "Crie seu primeiro plano de evolução.";
        GrowthPlanStep next = plan.getSteps().stream().filter(step -> !"DONE".equals(step.getStatus())).findFirst().orElse(null);
        if (next != null) return next.getTitle();
        if (evidenceCount == 0) return "Registre uma evidência do projeto que você concluiu.";
        if (applications == 0) return "Explore uma oportunidade e envie sua candidatura.";
        return "Acompanhe suas candidaturas e peça feedback.";
    }

    private OpportunityOsDto.PlanResponse toPlan(GrowthPlan plan) {
        if (plan == null) return null;
        List<OpportunityOsDto.StepResponse> steps = plan.getSteps().stream().map(step -> new OpportunityOsDto.StepResponse(
            step.getStepId(), step.getStepOrder(), step.getTitle(), step.getDescription(), step.getKind(),
            step.getSkillFocus(), step.getEstimatedHours(), step.getStatus(), step.getEvidenceId())).toList();
        return new OpportunityOsDto.PlanResponse(plan.getPlanId(), plan.getGoal(), plan.getTargetRole(), plan.getConstraints(),
            plan.getWeeklyHours(), plan.getStatus(), plan.getProgressPercent(), plan.getCreatedAt(), plan.getUpdatedAt(), steps);
    }

    private OpportunityOsDto.EvidenceResponse toEvidence(SkillEvidence evidence) {
        return new OpportunityOsDto.EvidenceResponse(evidence.getEvidenceId(), evidence.getTitle(), evidence.getSkillFocus(),
            evidence.getDescription(), evidence.getArtifactUrl(), evidence.getEvidenceType(), evidence.getVerificationStatus(),
            evidence.getOpportunity() == null ? null : evidence.getOpportunity().getOpportunityId(), evidence.getPlanStepId(),
            evidence.getCreatedAt(), evidence.getUpdatedAt());
    }

    private OpportunityOsDto.ApplicationResponse toApplication(OpportunityApplication application) {
        return new OpportunityOsDto.ApplicationResponse(application.getApplicationId(), application.getOpportunity().getOpportunityId(),
            application.getOpportunity().getOrganizationName(), application.getOpportunity().getTitle(), application.getStatus(),
            application.getMessage(), application.getCreatedAt());
    }

    private static int clampHours(Integer value) { return value == null ? 5 : Math.max(1, Math.min(40, value)); }
    private static String trim(String value) { return isBlank(value) ? null : value.trim(); }
    private static String required(String value, String message) {
        if (isBlank(value)) throw new IllegalArgumentException(message);
        return value.trim();
    }
    private static boolean isBlank(String value) { return value == null || value.isBlank(); }
}
