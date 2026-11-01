package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.ReportDTO;
import br.com.teamss.skillswap.skill_swap.model.services.ReportService;
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
}