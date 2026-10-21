package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.ModerationAppealRequest;
import br.com.teamss.skillswap.skill_swap.dto.ModerationCaseDTO;
import br.com.teamss.skillswap.skill_swap.dto.ModerationDecisionRequest;
import br.com.teamss.skillswap.skill_swap.dto.ModerationPolicyDTO;
import br.com.teamss.skillswap.skill_swap.dto.ReportDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.ModerationAction;
import br.com.teamss.skillswap.skill_swap.model.entities.Report;
import br.com.teamss.skillswap.skill_swap.model.entities.ReportStatus;
import br.com.teamss.skillswap.skill_swap.model.entities.ModerationSettings;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.ReportRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ModerationSettingsRepository;
import br.com.teamss.skillswap.skill_swap.model.services.AdminService;
import br.com.teamss.skillswap.skill_swap.model.services.ContentModerationService;
import br.com.teamss.skillswap.skill_swap.model.services.ModerationAssessment;
import br.com.teamss.skillswap.skill_swap.model.services.ReportService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ContentModerationService contentModerationService;
    private final AdminService adminService;
    private final UserServiceDTO userServiceDTO;
    private final ModerationSettingsRepository moderationSettingsRepository;

    public ReportServiceImpl(ReportRepository reportRepository,
                             UserRepository userRepository,
                             ContentModerationService contentModerationService,
                             AdminService adminService,
                             UserServiceDTO userServiceDTO,
                             ModerationSettingsRepository moderationSettingsRepository) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.contentModerationService = contentModerationService;
        this.adminService = adminService;
        this.userServiceDTO = userServiceDTO;
        this.moderationSettingsRepository = moderationSettingsRepository;
    }

    @Override
    @Transactional
    public Report createReport(ReportDTO reportDTO) {
        User reporter = userRepository.findById(reportDTO.getReporterId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário reportando não encontrado."));
        User reportedUser = userRepository.findById(reportDTO.getReportedUserId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário denunciado não encontrado."));

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedUser(reportedUser);
        report.setReason(reportDTO.getReason());
        report.setReportedContent(reportDTO.getReportedContent());
        String textForAssessment = reportDTO.getReportedContent() == null || reportDTO.getReportedContent().isBlank()
                ? reportDTO.getReason() : reportDTO.getReportedContent();
        ModerationAssessment assessment = contentModerationService.assess(textForAssessment);
        applyAssessment(report, assessment);
        applyConfiguredTemporaryContainment(report, assessment);
        return reportRepository.save(report);
    }

    @Override
    @Transactional
    public Report createAutomatedCase(User reportedUser, String content, String context,
                                      ModerationAssessment assessment) {
        Report report = new Report();
        // O próprio autor é mantido como referência técnica do conteúdo; a
        // origem AI_CONTENT_BLOCK deixa claro que não foi uma denúncia dele.
        report.setReporter(reportedUser);
        report.setReportedUser(reportedUser);
        report.setReason("Conteúdo contido automaticamente em " + context + ".");
        report.setReportedContent(content);
        report.setSource("AI_CONTENT_BLOCK");
        applyAssessment(report, assessment);
        applyConfiguredTemporaryContainment(report, assessment);
        return reportRepository.save(report);
    }

    @Override
    public List<Report> getPendingReports() {
        return reportRepository.findByStatus(ReportStatus.PENDING);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModerationCaseDTO> getModerationQueue() {
        return reportRepository.findByStatusInOrderByCreatedAtAsc(
                        List.of(ReportStatus.PENDING, ReportStatus.IN_REVIEW, ReportStatus.APPEALED))
                .stream()
                .map(ModerationCaseDTO::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ModerationPolicyDTO getModerationPolicy() {
        ModerationSettings settings = moderationSettingsRepository.findById(1)
                .orElseGet(ModerationSettings::new);
        return new ModerationPolicyDTO(settings.isAutomaticTemporaryBanEnabled(), settings.getAutomaticTemporaryBanHours());
    }

    @Override
    @Transactional
    public ModerationPolicyDTO updateModerationPolicy(ModerationPolicyDTO policy) {
        ModerationSettings settings = moderationSettingsRepository.findById(1)
                .orElseGet(ModerationSettings::new);
        settings.setAutomaticTemporaryBanEnabled(policy.automaticTemporaryBanEnabled());
        settings.setAutomaticTemporaryBanHours(policy.automaticTemporaryBanHours());
        settings.setUpdatedAt(Instant.now());
        ModerationSettings saved = moderationSettingsRepository.save(settings);
        return new ModerationPolicyDTO(saved.isAutomaticTemporaryBanEnabled(), saved.getAutomaticTemporaryBanHours());
    }

    /** Reavalia a triagem e só pode aplicar contenção temporária se a política do painel estiver ligada. */
    @Override
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void processReports() {
        getPendingReports().forEach(report -> {
            String textForAssessment = report.getReportedContent() == null || report.getReportedContent().isBlank()
                    ? report.getReason() : report.getReportedContent();
            ModerationAssessment assessment = contentModerationService.assess(textForAssessment);
            applyAssessment(report, assessment);
            applyConfiguredTemporaryContainment(report, assessment);
            reportRepository.save(report);
        });
    }

    @Override
    @Transactional
    public ModerationCaseDTO decide(Long reportId, ModerationDecisionRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Caso de moderação não encontrado."));
        User reviewer = authenticatedUser();

        if (!request.humanConfirmation()) {
            throw new IllegalArgumentException("A decisão precisa ser confirmada por um administrador humano.");
        }
        if (report.getStatus() == ReportStatus.RESOLVED && !"PENDING".equalsIgnoreCase(report.getAppealStatus())) {
            throw new IllegalStateException("Este caso já foi resolvido.");
        }

        Instant expiresAt = null;
        if (request.action() == ModerationAction.SUSPEND_DAYS) {
            validateDuration(request.durationDays(), 1, 31, "dias");
            expiresAt = Instant.now().plus(request.durationDays(), ChronoUnit.DAYS);
        } else if (request.action() == ModerationAction.SUSPEND_MONTHS) {
            validateDuration(request.durationDays(), 30, 3650, "dias equivalentes a meses");
            expiresAt = Instant.now().plus(request.durationDays(), ChronoUnit.DAYS);
        }

        switch (request.action()) {
            case RELEASE, WARNING -> adminService.unbanUser(report.getReportedUser().getUserId());
            case SUSPEND_DAYS, SUSPEND_MONTHS, PERMANENT_BAN -> adminService.banUser(
                    report.getReportedUser().getUserId(), request.reason(), expiresAt, null);
        }

        report.setAction(request.action());
        report.setReviewedBy(reviewer);
        report.setReviewedAt(Instant.now());
        report.setDecisionReason(request.reason().trim());
        report.setDecisionEvidence(request.evidence() == null ? "" : request.evidence().trim());
        report.setStatus(ReportStatus.RESOLVED);
        if ("PENDING".equalsIgnoreCase(report.getAppealStatus())) {
            report.setAppealStatus("RESOLVED");
        }
        return ModerationCaseDTO.from(reportRepository.save(report));
    }

    @Override
    @Transactional
    public ModerationCaseDTO submitAppeal(Long reportId, ModerationAppealRequest request) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Caso de moderação não encontrado."));
        User currentUser = authenticatedUser();
        if (!report.getReportedUser().getUserId().equals(currentUser.getUserId())) {
            throw new SecurityException("Somente a pessoa afetada pode apresentar recurso.");
        }
        if (report.getAction() == null) {
            throw new IllegalStateException("Não há decisão administrativa para recorrer.");
        }
        report.setAppealStatus("PENDING");
        report.setAppealReason(request.reason().trim());
        report.setAppealedAt(Instant.now());
        report.setStatus(ReportStatus.APPEALED);
        return ModerationCaseDTO.from(reportRepository.save(report));
    }

    private void applyAssessment(Report report, ModerationAssessment assessment) {
        report.setAiCategory(assessment.category());
        report.setAiConfidence(BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, assessment.confidence()))));
        report.setAiRecommendation(assessment.recommendation());
        report.setContainedUntil(assessment.blocked() ? Instant.now().plus(1, ChronoUnit.HOURS) : null);
    }

    private void applyConfiguredTemporaryContainment(Report report, ModerationAssessment assessment) {
        ModerationSettings settings = moderationSettingsRepository.findById(1).orElseGet(ModerationSettings::new);
        if (!settings.isAutomaticTemporaryBanEnabled() || !assessment.blocked()
                || !"BLOCK".equalsIgnoreCase(assessment.recommendation())
                || assessment.confidence() < 0.85
                || report.isAutomaticTemporaryBanApplied() || isAdministrator(report.getReportedUser())) {
            return;
        }
        Instant expiresAt = Instant.now().plus(settings.getAutomaticTemporaryBanHours(), ChronoUnit.HOURS);
        adminService.banUser(report.getReportedUser().getUserId(),
                "Contenção temporária automática enquanto o caso aguarda revisão humana.", expiresAt, null);
        report.setAutomaticTemporaryBanApplied(true);
        report.setAutomaticActionAt(Instant.now());
        report.setAutomaticActionReason("Contenção temporária de emergência; não é decisão final.");
    }

    private boolean isAdministrator(User user) {
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()));
    }

    private User authenticatedUser() {
        var current = userServiceDTO.getAuthenticatedUser();
        return userRepository.findById(current.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("Administrador autenticado não encontrado."));
    }

    private void validateDuration(Integer value, int min, int max, String unit) {
        if (value == null || value < min || value > max) {
            throw new IllegalArgumentException("A duração deve estar entre " + min + " e " + max + " " + unit + ".");
        }
    }
}
