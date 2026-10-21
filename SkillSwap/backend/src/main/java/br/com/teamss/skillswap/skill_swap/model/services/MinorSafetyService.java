package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.LessonRepository;
import java.sql.Date;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

/** Regras mínimas de proteção para interação direta envolvendo menores. */
@Service
public class MinorSafetyService {
    private final LessonRepository lessonRepository;

    public MinorSafetyService(LessonRepository lessonRepository) {
        this.lessonRepository = lessonRepository;
    }

    public boolean isMinor(User user) {
        if (user == null) return false;
        if ("MINOR".equalsIgnoreCase(user.getAccountAgeBand())) return true;
        Date birthDate = user.getBirthDate();
        return birthDate != null && Period.between(birthDate.toLocalDate(), LocalDate.now()).getYears() < 18;
    }

    public boolean hasLessonRelationship(UUID firstUser, UUID secondUser) {
        List<Lesson> lessons = lessonRepository.findBetweenUsers(firstUser, secondUser);
        return lessons.stream().anyMatch(lesson -> {
            String status = lesson.getStatus();
            return status == null || (!"CANCELLED".equalsIgnoreCase(status)
                && !"CANCELED".equalsIgnoreCase(status)
                && !"REJECTED".equalsIgnoreCase(status)
                && !"BLOCKED".equalsIgnoreCase(status));
        });
    }

    /**
     * Adultos e menores não iniciam chat, voz ou arquivos por contato arbitrário.
     * A interação fica disponível quando existe uma aula registrada entre as partes.
     * Contatos entre adultos e entre menores continuam disponíveis.
     */
    public void assertDirectInteractionAllowed(User sender, User receiver) {
        if (sender == null || receiver == null || sender.getUserId() == null || receiver.getUserId() == null) {
            throw new AccessDeniedException("Participantes inválidos.");
        }
        boolean adultMinorPair = isMinor(sender) ^ isMinor(receiver);
        if (adultMinorPair && !hasLessonRelationship(sender.getUserId(), receiver.getUserId())) {
            throw new AccessDeniedException("Para proteger menores, esta interação precisa estar vinculada a uma aula registrada.");
        }
    }
}
