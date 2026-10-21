package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.entities.KarmaEvent;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.KarmaEventRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.KarmaService;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KarmaServiceImpl implements KarmaService {
    private final KarmaEventRepository eventRepository;
    private final UserRepository userRepository;

    public KarmaServiceImpl(KarmaEventRepository eventRepository, UserRepository userRepository) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void award(UUID userId, long points, String reason, String idempotencyKey) {
        if (points <= 0) throw new IllegalArgumentException("A pontuacao Karma deve ser positiva.");
        if (eventRepository.existsByIdempotencyKey(idempotencyKey)) return;
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado."));
        user.setKarmaPoints(Math.addExact(user.getKarmaPoints(), points));
        userRepository.save(user);
        KarmaEvent event = new KarmaEvent();
        event.setUser(user);
        event.setPoints(points);
        event.setReason(reason);
        event.setIdempotencyKey(idempotencyKey);
        eventRepository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public long getPoints(UUID userId) {
        return userRepository.findById(userId).map(User::getKarmaPoints)
            .orElseThrow(() -> new EntityNotFoundException("Usuario nao encontrado."));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KarmaEvent> getRecentEvents(UUID userId) {
        return eventRepository.findTop20ByUser_UserIdOrderByCreatedAtDesc(userId);
    }
}
