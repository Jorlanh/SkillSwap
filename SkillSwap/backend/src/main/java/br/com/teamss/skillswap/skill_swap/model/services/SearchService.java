package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.SearchResultDTO;
import java.io.IOException;
import java.util.List;

public interface SearchService {
    List<SearchResultDTO> search(String query, String filter, String sortBy) throws IOException;
    <T> void indexDocument(String indexName, String docId, T document) throws IOException;
    void deleteDocument(String indexName, String docId) throws IOException;
}