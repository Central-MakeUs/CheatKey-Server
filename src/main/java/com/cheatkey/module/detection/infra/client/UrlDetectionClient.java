package com.cheatkey.module.detection.infra.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrlDetectionClient {

    @Value("${google.safe-browsing.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public boolean checkUrl(String url) {
        String apiUrl = "https://safebrowsing.googleapis.com/v4/threatMatches:find?key=" + apiKey;

        Map<String, Object> client = Map.of(
                "clientId", "cheatkey",
                "clientVersion", "1.0"
        );

        Map<String, Object> threatInfo = new HashMap<>();
        threatInfo.put("threatTypes", List.of("MALWARE", "SOCIAL_ENGINEERING", "UNWANTED_SOFTWARE", "POTENTIALLY_HARMFUL_APPLICATION"));
        threatInfo.put("platformTypes", List.of("ANY_PLATFORM"));
        threatInfo.put("threatEntryTypes", List.of("URL"));
        threatInfo.put("threatEntries", List.of(Map.of("url", url)));

        Map<String, Object> requestBody = Map.of(
                "client", client,
                "threatInfo", threatInfo
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        try {
            Map<?, ?> response = restTemplate.postForObject(apiUrl, request, Map.class);
            return response != null && response.containsKey("matches");
        } catch (Exception e) {
            log.error("Google Safe Browsing API 호출 실패", e);
            return false; // 실패 시 기본적으로 안전하다고 간주
        }
    }
}
