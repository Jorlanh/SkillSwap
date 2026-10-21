package br.com.teamss.skillswap.skill_swap.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future; // Adicionado para scheduledTime
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    private String title;

    private String introduction;

    @Min(value = 15, message = "A duração mínima da aula é de 15 minutos")
    @Max(value = 480, message = "A duração máxima da aula é de 8 horas")
    private int durationMinutes = 60;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }
}
