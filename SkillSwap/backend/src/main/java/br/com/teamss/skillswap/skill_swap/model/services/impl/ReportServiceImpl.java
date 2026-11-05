package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.ReportDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Report;
import br.com.teamss.skillswap.skill_swap.model.entities.ReportStatus;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.ReportRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.AdminService;
import br.com.teamss.skillswap.skill_swap.model.services.ContentModerationService;
import br.com.teamss.skillswap.skill_swap.model.services.NotificationService;
import br.com.teamss.skillswap.skill_swap.model.services.ReportService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final ContentModerationService contentModerationService;
    private final AdminService adminService;
    private final NotificationService notificationService;

    public ReportServiceImpl(ReportRepository reportRepository, UserRepository userRepository,
                             ContentModerationService contentModerationService, AdminService adminService,
                             NotificationService notificationService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.contentModerationService = contentModerationService;
        this.adminService = adminService;
        this.notificationService = notificationService;
    }

    @Override
    public Report createReport(ReportDTO reportDTO) {
        User reporter = userRepository.findById(reportDTO.getReporterId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário reportando não encontrado."));
        User reportedUser = userRepository.findById(reportDTO.getReportedUserId())
                .orElseThrow(() -> new EntityNotFoundException("Usuário denunciado não encontrado."));

        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedUser(reportedUser);
        report.setReason(reportDTO.getReason());
        report.setTargetType(reportDTO.getTargetType() == null || reportDTO.getTargetType().isBlank() ? "USER" : reportDTO.getTargetType().trim().toUpperCase());
        report.setTargetId(reportDTO.getTargetId());
        report.setCategory(reportDTO.getCategory());
        return reportRepository.save(report);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<Report> getPendingReports() {
        return reportRepository.findByStatus(ReportStatus.PENDING);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void reviewReport(Long reportId, ReportStatus status, String responseMessage) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Denúncia não encontrada."));
        if (status == null || status == ReportStatus.PENDING) {
            throw new IllegalArgumentException("Informe uma decisão válida para a denúncia.");
        }
        report.setStatus(status);
        reportRepository.save(report);
        if (responseMessage != null && !responseMessage.isBlank() && report.getReportedUser() != null) {
            notificationService.createNotification(report.getReportedUser().getUserId(), responseMessage.trim());
        }
    }

    @Override
    @Scheduled(fixedRate = 3600000) // Executa a cada hora
    public void processReports() {
        List<Report> pendingReports = getPendingReports();
        for (Report report : pendingReports) {
            boolean isContentInappropriate = contentModerationService.isContentInappropriate(report.getReason());
            boolean reportedUserIsAdmin = report.getReportedUser().getRoles().stream()
                    .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()) || "ROLE_ADMIN".equalsIgnoreCase(role.getName()));
            if (isContentInappropriate && !reportedUserIsAdmin) {
                adminService.banUser(report.getReportedUser().getUserId(),
                                     "Violação das diretrizes da comunidade (detectado automaticamente).",
                                     Instant.now().plus(24, ChronoUnit.HOURS),
                                     null, "GENERAL");
                report.setStatus(ReportStatus.ACTION_TAKEN);
            } else {
                // A classificação automática nunca aplica banimento permanente a uma conta administrativa.
                report.setStatus(ReportStatus.REVIEWED);
            }
            reportRepository.save(report);
        }
    }
}
