package com.cheatkey.module.detection.infra.client;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.detection.interfaces.dto.SafeBrowsingResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UrlDetectionClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${google.safe-browsing.api-key}")
    private String apiKey;

    public boolean checkUrl(String url) {
        String uri = "/v4/threatMatches:find?key=" + apiKey;

        WebClient webClient = webClientBuilder
                .baseUrl("https://safebrowsing.googleapis.com")
                .defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON))
                .build();

        SafeBrowsingResponse response;
        try {
            // ========================== 중복 요청 방지용 Redis TTL 캐시 Hook ==========================
            // 추후 RedisTemplate 등을 주입받아 이 위치에서 다음 로직을 추가할 수 있습니다:
            // 1. (key: "safe-url::" + url) 캐시 조회
            // 2. 값이 존재하면 해당 값으로 반환 (API 호출 생략)
            // 3. 값이 없으면 아래 API 호출 → 결과를 TTL 3~6시간으로 캐시 저장
            // ===========================================================================================

            response = webClient.post()
                    .uri(uri)
                    .bodyValue(buildRequestBody(url))
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, clientResponse ->
                            clientResponse.bodyToMono(String.class)
                                    .map(body -> {
                                        log.error("Google Safe Browsing API error: {}", body);
                                        return new CustomException(ErrorCode.GOOGLE_API_UNAVAILABLE);
                                    }))
                    .bodyToMono(SafeBrowsingResponse.class)
                    .block();

        } catch (WebClientResponseException e) {
            log.error("Google API 응답 오류: {}", e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorCode.GOOGLE_API_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Google API 호출 실패", e);
            throw new CustomException(ErrorCode.GOOGLE_API_UNAVAILABLE);
        }

        // NOTE: Google Safe Browsing API에서 제공하는 세부 위협 정보(threatType, platformType, threatEntryType)는
        // 현재 서비스에서는 단순 위험/안전 구분만 사용하므로 Enum 정의 없이 String으로 처리
        // 향후 정책 변경 시 위협 타입별 세분화 고려 가능
        return Optional.ofNullable(response)
                .map(SafeBrowsingResponse::getMatches)
                .map(matches -> !matches.isEmpty())
                .orElse(false);
    }

    private Map<String, Object> buildRequestBody(String url) {
        return Map.of(
                "client", Map.of(
                        "clientId", "cheatkey",
                        "clientVersion", "1.0"
                ),
                "threatInfo", Map.of(
                        "threatTypes", List.of(
                                "MALWARE",
                                "SOCIAL_ENGINEERING",
                                "UNWANTED_SOFTWARE",
                                "POTENTIALLY_HARMFUL_APPLICATION"
                        ),
                        "platformTypes", List.of("ANY_PLATFORM"),
                        "threatEntryTypes", List.of("URL"),
                        "threatEntries", List.of(Map.of("url", url))
                )
        );
    }
}
