package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.ReportDTO;
import br.com.teamss.skillswap.skill_swap.dto.AdminReportDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.ReportStatus;
import br.com.teamss.skillswap.skill_swap.model.services.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    public ResponseEntity<Void> createReport(@RequestBody ReportDTO reportDTO) {
        reportService.createReport(reportDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AdminReportDTO>> pendingReports() {
        return ResponseEntity.ok(reportService.getPendingReports().stream().map(report ->
                new AdminReportDTO(report.getId(), report.getReporter().getUserId(), report.getReporter().getUsername(),
                        report.getReportedUser().getUserId(), report.getReportedUser().getUsername(), report.getReason(),
                        report.getStatus().name(), report.getCreatedAt())).toList());
    }

    @PostMapping("/{reportId}/review")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> reviewReport(@PathVariable Long reportId,
                                             @RequestParam ReportStatus status) {
        reportService.reviewReport(reportId, status);
        return ResponseEntity.noContent().build();
    }
}
