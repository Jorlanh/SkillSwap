package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.model.entities.KarmaEvent;
import java.util.List;
import java.util.UUID;

public interface KarmaService {
    void award(UUID userId, long points, String reason, String idempotencyKey);
    long getPoints(UUID userId);
    List<KarmaEvent> getRecentEvents(UUID userId);
}
