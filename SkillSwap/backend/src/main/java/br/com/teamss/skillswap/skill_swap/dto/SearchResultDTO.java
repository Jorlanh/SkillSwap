package br.com.teamss.skillswap.skill_swap.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchResultDTO {
    private String id;
    private String type; // "USER", "POST", "COMMUNITY"
    private String title;
    private String description;
    private String imageUrl;
    private double score; // Para ordenação por relevância
}