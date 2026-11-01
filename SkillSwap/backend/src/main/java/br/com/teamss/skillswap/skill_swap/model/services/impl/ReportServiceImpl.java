package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.ReportDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Report;
import br.com.teamss.skillswap.skill_swap.model.entities.ReportStatus;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.ReportRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.AdminService;
import br.com.teamss.skillswap.skill_swap.model.services.ContentModerationService;
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

    public ReportServiceImpl(ReportRepository reportRepository, UserRepository userRepository,
                             ContentModerationService contentModerationService, AdminService adminService) {
        this.reportRepository = reportRepository;
        this.userRepository = userRepository;
        this.contentModerationService = contentModerationService;
        this.adminService = adminService;
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
        return reportRepository.save(report);
    }

    @Override
    public List<Report> getPendingReports() {
        return reportRepository.findByStatus(ReportStatus.PENDING);
    }

    @Override
    @Scheduled(fixedRate = 3600000) // Executa a cada hora
    public void processReports() {
        List<Report> pendingReports = getPendingReports();
        for (Report report : pendingReports) {
            boolean isContentInappropriate = contentModerationService.isContentInappropriate(report.getReason());
            if (isContentInappropriate) {
                adminService.banUser(report.getReportedUser().getUserId(),
                                     "Violação das diretrizes da comunidade (detectado automaticamente).",
                                     Instant.now().plus(24, ChronoUnit.HOURS),
                                     null);
                report.setStatus(ReportStatus.ACTION_TAKEN);
            } else {
                report.setStatus(ReportStatus.REVIEWED);
            }
            reportRepository.save(report);
        }
    }
}