package br.com.teamss.skillswap.skill_swap.model.services.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.SortOptions;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.mapping.FieldType;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.TotalHits;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import br.com.teamss.skillswap.skill_swap.dto.SearchResultDTO;
import br.com.teamss.skillswap.skill_swap.model.services.SearchService;

@Service
public class SearchServiceImpl implements SearchService {

    private final OpenSearchClient client;

    public static final String USER_INDEX = "users";
    public static final String POST_INDEX = "posts";
    public static final String COMMUNITY_INDEX = "communities";

    public SearchServiceImpl(OpenSearchClient client) {
        this.client = client;
    }

    @Override
    public List<SearchResultDTO> search(String query, String filter, String sortBy) throws IOException {
        List<String> indicesToSearch = new ArrayList<>();
        
        // Configuração de filtros de índice
        if ("ALL".equalsIgnoreCase(filter)) {
            indicesToSearch.addAll(List.of(USER_INDEX, POST_INDEX, COMMUNITY_INDEX));
        } else if ("PROFILE".equalsIgnoreCase(filter)) {
            indicesToSearch.add(USER_INDEX);
        } else if ("CONTENT".equalsIgnoreCase(filter)) {
            indicesToSearch.add(POST_INDEX);
        } else if ("COMMUNITY".equalsIgnoreCase(filter)) {
            indicesToSearch.add(COMMUNITY_INDEX);
        }

        // Busca Full-Text com Fuzziness (Tolera erros de digitação)
        Query multiMatchQuery = Query.of(q -> q
            .multiMatch(mm -> mm
                .query(query)
                .fields("username^3", "name^2", "bio", "title^3", "content", "description")
                .fuzziness("AUTO")
            )
        );

        SearchRequest.Builder requestBuilder = new SearchRequest.Builder()
            .index(indicesToSearch)
            .query(multiMatchQuery);

        // Lógica de Ordenação
        List<SortOptions> sortOptions = new ArrayList<>();
        if ("DATE".equalsIgnoreCase(sortBy)) {
            sortOptions.add(SortOptions.of(s -> s.field(f -> f
                .field("createdAt")
                .order(SortOrder.Desc)
                .unmappedType(FieldType.Long)
                .missing(FieldValue.of("_last"))
            )));
        } else {
            // Relevância (Score) é o padrão
            sortOptions.add(SortOptions.of(s -> s.score(sc -> sc.order(SortOrder.Desc))));
        }

        requestBuilder.sort(sortOptions);

        SearchResponse<JsonNode> response = client.search(requestBuilder.build(), JsonNode.class);

        TotalHits total = response.hits().total();
        if (total == null || total.value() == 0) {
            return new ArrayList<>();
        }

        return response.hits().hits().stream()
            .map(this::convertHitToDTO)
            .collect(Collectors.toList());
    }

    private SearchResultDTO convertHitToDTO(Hit<JsonNode> hit) {
        String index = hit.index();
        String type = mapIndexToType(index);
        JsonNode source = hit.source();
        
        if (source == null) return null;

        String id = hit.id();
        String title = "";
        String description = "";
        String imageUrl = "";

        // Mapeamento dinâmico baseado no índice de origem
        switch (type) {
            case "user":
                title = source.path("username").asText("");
                description = source.path("name").asText("") + " - " + source.path("bio").asText("");
                imageUrl = source.path("imageUrl").asText("");
                break;
            case "post":
                title = source.path("title").asText("");
                description = source.path("content").asText("");
                imageUrl = source.path("imageUrl").asText("");
                break;
            case "community":
                title = source.path("name").asText("");
                description = source.path("description").asText("");
                break;
        }

        Double scoreObj = hit.score();
        double score = (scoreObj != null) ? scoreObj : 0.0;

        return new SearchResultDTO(id, type, title, description, imageUrl, score);
    }

    private String mapIndexToType(String index) {
        if (USER_INDEX.equals(index)) return "user";
        if (POST_INDEX.equals(index)) return "post";
        if (COMMUNITY_INDEX.equals(index)) return "community";
        return "unknown";
    }

    @Override
    public <T> void indexDocument(String indexName, String docId, T document) throws IOException {
        client.index(i -> i.index(indexName).id(docId).document(document));
    }

    @Override
    public void deleteDocument(String indexName, String docId) throws IOException {
        client.delete(d -> d.index(indexName).id(docId));
    }
}