package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.ReportDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Report;
import java.util.List;

public interface ReportService {
    Report createReport(ReportDTO reportDTO);
    List<Report> getPendingReports();
    void processReports();
}