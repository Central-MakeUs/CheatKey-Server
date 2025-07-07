package com.cheatkey.module.detection.infra.adapter;

import com.cheatkey.module.detection.infra.client.VectorDbClient;
import com.cheatkey.module.detection.interfaces.dto.vector.EmbeddingRequest;
import com.cheatkey.module.detection.interfaces.dto.vector.EmbeddingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class VectorDbAdapter implements VectorDbClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${qdrant.host}")
    private String QDRANT_HOST;

    @Value("${embedding.api.url}")
    private String EMBEDDING_API_URL;
    private final String COLLECTION = "phishing_cases";

    @Override
    public List<Float> embed(String text) {
        EmbeddingRequest request = new EmbeddingRequest(text);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<EmbeddingRequest> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<EmbeddingResponse> response = restTemplate.postForEntity(
                    EMBEDDING_API_URL,
                    entity,
                    EmbeddingResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody().getVector();
            } else {
                throw new IllegalStateException("임베딩 API 응답 실패: " + response.getStatusCode());
            }
        } catch (Exception e) {
            throw new RuntimeException("임베딩 API 호출 중 오류", e);
        }
    }

    @Override
    public List<SearchResult> searchSimilarCases(List<Float> embedding, int topK) {
        Map<String, Object> body = Map.of(
                "vector", embedding,
                "top", topK
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
                QDRANT_HOST + "/collections/" + COLLECTION + "/points/search",
                new HttpEntity<>(body),
                Map.class
        );

        List<Map<String, Object>> results = (List<Map<String, Object>>) response.getBody().get("result");

        return results.stream().map(result -> {
            Map<String, Object> payload = (Map<String, Object>) result.get("payload");
            Double score = (Double) result.get("score");
            Integer id = (Integer) result.get("id");
            return new SearchResult(id.longValue(), score.floatValue(), payload);
        }).toList();
    }

    @Override
    public void saveVector(Long id, List<Float> vector, Map<String, Object> payload) {
        String url = QDRANT_HOST + "/collections/" + COLLECTION + "/points";

        Map<String, Object> point = Map.of(
                "id", id,
                "vector", vector,
                "payload", payload
        );

        Map<String, Object> requestBody = Map.of(
                "points", List.of(point)
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url,
                    HttpMethod.PUT,
                    entity,
                    Map.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Qdrant 저장 실패: " + response.getStatusCode());
            }

        } catch (Exception e) {
            throw new RuntimeException("Qdrant 저장 중 오류", e);
        }
    }

}

