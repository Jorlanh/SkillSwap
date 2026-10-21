package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.SearchResultDTO;
import br.com.teamss.skillswap.skill_swap.model.services.SearchService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "http://localhost:8081") // Garantia adicional de CORS
public class SearchController {

    private final SearchService searchService;

    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    @GetMapping
    public ResponseEntity<List<SearchResultDTO>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "ALL") String filter,
            @RequestParam(defaultValue = "RELEVANCE") String sortBy) throws IOException {
        
        List<SearchResultDTO> results = searchService.search(query, filter, sortBy);
        return ResponseEntity.ok(results);
    }
}