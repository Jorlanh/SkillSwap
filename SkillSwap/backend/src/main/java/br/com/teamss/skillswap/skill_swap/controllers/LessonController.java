package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.LessonCertificateResponse;
import br.com.teamss.skillswap.skill_swap.dto.LessonCompletionResponse;
import br.com.teamss.skillswap.skill_swap.dto.LessonScheduleRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import br.com.teamss.skillswap.skill_swap.model.services.LessonService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lesson")
@PreAuthorize("isAuthenticated()")
public class LessonController {
    private final LessonService lessonService;
    private final UserServiceDTO userServiceDTO;

    public LessonController(LessonService lessonService, UserServiceDTO userServiceDTO) {
        this.lessonService = lessonService;
        this.userServiceDTO = userServiceDTO;
    }

    @PostMapping("/schedule")
    public ResponseEntity<Lesson> scheduleLesson(@Valid @RequestBody LessonScheduleRequestDTO lessonRequest) {
        return ResponseEntity.ok(lessonService.scheduleLesson(lessonRequest));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<Lesson>> getMine() {
        UUID userId = userServiceDTO.getAuthenticatedUser().getUserId();
        return ResponseEntity.ok(lessonService.getLessonsForUser(userId));
    }

    @GetMapping("/upcoming/{userId}")
    public ResponseEntity<List<Lesson>> getUpcomingLessons(@PathVariable UUID userId) {
        UUID currentUserId = userServiceDTO.getAuthenticatedUser().getUserId();
        if (!currentUserId.equals(userId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(lessonService.getUpcomingLessons(userId));
    }

    @PatchMapping("/{lessonId}/complete")
    public ResponseEntity<LessonCompletionResponse> confirmCompletion(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.confirmCompletion(lessonId));
    }

    @GetMapping("/{lessonId}/certificate")
    public ResponseEntity<LessonCertificateResponse> getCertificate(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.getCertificate(lessonId));
    }

    @PostMapping("/notify/{lessonId}")
    public ResponseEntity<Void> notifyLesson(@PathVariable Long lessonId) {
        lessonService.notifyLesson(lessonId);
        return ResponseEntity.ok().build();
    }
}
