package br.com.teamss.skillswap.skill_swap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SkillSwapApplication {
    public static void main(String[] args) {
        SpringApplication.run(SkillSwapApplication.class, args);
    }
}