package br.com.teamss.skillswap.skill_swap.dto;

import br.com.teamss.skillswap.skill_swap.model.entities.KarmaEvent;
import java.time.Instant;

public record KarmaEventDTO(long points, String reason, Instant createdAt) {
    public static KarmaEventDTO from(KarmaEvent event) {
        return new KarmaEventDTO(event.getPoints(), event.getReason(), event.getCreatedAt());
    }
}
