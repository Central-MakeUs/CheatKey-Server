package com.cheatkey.module.detection.infra.client;

import java.util.List;
import java.util.Map;

public interface VectorDbClient {
    List<Float> embed(String text);
    List<SearchResult> searchSimilarCases(List<Float> embedding, int topK);
    void saveVector(String id, List<Float> vector, Map<String, Object> payload);

    record SearchResult(String id, float score, Map<String, Object> payload) {}
}
