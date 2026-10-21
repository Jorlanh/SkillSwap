package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_achievements")
@Getter
@Setter
@NoArgsConstructor
public class Achievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String achievementKey; // Ex: "10_TRADES_COMPLETED"

    @Column(nullable = false)
    private String name; // Ex: "Mestre das Trocas"

    @Column(nullable = false)
    private String description; // Ex: "Concluiu 10 trocas de habilidades com sucesso."
}