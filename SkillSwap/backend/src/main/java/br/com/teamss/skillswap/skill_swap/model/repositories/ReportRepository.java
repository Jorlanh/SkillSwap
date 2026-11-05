package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.Report;
import br.com.teamss.skillswap.skill_swap.model.entities.ReportStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    @EntityGraph(attributePaths = {"reporter", "reportedUser"})
    List<Report> findByStatus(ReportStatus status);
}
