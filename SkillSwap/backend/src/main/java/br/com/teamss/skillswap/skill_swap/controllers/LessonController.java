package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.LessonScheduleRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.LessonCertificateDTO;
import br.com.teamss.skillswap.skill_swap.dto.LessonViewDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import br.com.teamss.skillswap.skill_swap.model.services.LessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity; // Import ResponseEntity
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/lesson")
public class LessonController {

    @Autowired
    private LessonService lessonService;

    @PostMapping("/schedule")
    public ResponseEntity<Lesson> scheduleLesson(@Valid @RequestBody LessonScheduleRequestDTO lessonRequest) {
        System.out.println("DTO recebido no controlador: " + lessonRequest);
        Lesson scheduledLesson = lessonService.scheduleLesson(lessonRequest);
        return ResponseEntity.ok(scheduledLesson); // Retorna ResponseEntity
    }

    @GetMapping("/upcoming/{userId}")
    public ResponseEntity<List<Lesson>> getUpcomingLessons(@PathVariable UUID userId) { // Retorna ResponseEntity
        List<Lesson> lessons = lessonService.getUpcomingLessons(userId);
        return ResponseEntity.ok(lessons);
    }

    @GetMapping("/mine")
    public ResponseEntity<List<LessonViewDTO>> getMyLessons() {
        return ResponseEntity.ok(lessonService.getMyLessons());
    }

    @PostMapping("/{lessonId}/complete")
    public ResponseEntity<LessonCertificateDTO> completeLesson(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.completeLesson(lessonId));
    }

    @GetMapping("/certificate/{lessonId}")
    public ResponseEntity<LessonCertificateDTO> getCertificate(@PathVariable Long lessonId) {
        return ResponseEntity.ok(lessonService.getCertificate(lessonId));
    }

    @PostMapping("/notify/{lessonId}")
    public ResponseEntity<Void> notifyLesson(@PathVariable Long lessonId) { // Retorna ResponseEntity
        lessonService.notifyLesson(lessonId);
        return ResponseEntity.ok().build(); // Retorna 200 OK sem corpo
    }
}
