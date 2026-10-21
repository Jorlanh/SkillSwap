package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.AccessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AccessLogRepository extends JpaRepository<AccessLog, Long> {
    List<AccessLog> findByUserIdOrderByAccessTimeDesc(UUID userId);
}