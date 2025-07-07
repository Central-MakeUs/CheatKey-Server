package com.cheatkey.module.detection.infra.client;

import java.util.List;
import java.util.Map;

public interface VectorDbClient {
    List<Float> embed(String text);
    List<SearchResult> searchSimilarCases(List<Float> embedding, int topK);
    void saveVector(Long id, List<Float> vector, Map<String, Object> payload);

    record SearchResult(Long id, float score, Map<String, Object> payload) {}
}
