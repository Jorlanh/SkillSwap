package br.com.teamss.skillswap.skill_swap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future; // Adicionado para scheduledTime
import java.time.Instant;
import java.util.UUID;

public class LessonScheduleRequestDTO {
    @NotNull(message = "teacherId não pode ser nulo")
    @JsonProperty("teacherId")
    private UUID teacherId;

    @NotNull(message = "studentId não pode ser nulo")
    @JsonProperty("studentId")
    private UUID studentId;

    @NotNull(message = "scheduledTime não pode ser nulo")
    @Future(message = "A data/hora agendada deve ser no futuro") // Garante que a aula seja agendada para o futuro
    @JsonProperty("scheduledTime")
    private Instant scheduledTime;

    // Getters e Setters
    public UUID getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(UUID teacherId) {
        this.teacherId = teacherId;
    }

    public UUID getStudentId() {
        return studentId;
    }

    public void setStudentId(UUID studentId) {
        this.studentId = studentId;
    }

    public Instant getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(Instant scheduledTime) {
        this.scheduledTime = scheduledTime;
    }
}
