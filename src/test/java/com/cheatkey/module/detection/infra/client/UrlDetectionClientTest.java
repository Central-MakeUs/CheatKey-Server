package com.cheatkey.module.detection.infra.client;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.module.detection.interfaces.dto.SafeBrowsingMatch;
import com.cheatkey.module.detection.interfaces.dto.SafeBrowsingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UrlDetectionClientTest {

    @InjectMocks
    private UrlDetectionClient client;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestBodyUriSpec requestSpec;

    @Mock
    private WebClient.RequestBodySpec bodySpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> headersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Value("${google.safe-browsing.api-key}")
    private String apiKey = "dummy-key";

    @BeforeEach
    void setup() {
        client = new UrlDetectionClient(webClientBuilder);
    }

    @Test
    void checkUrl_위협탐지됨_true반환() {
        // given
        SafeBrowsingResponse response = new SafeBrowsingResponse();
        SafeBrowsingMatch match = new SafeBrowsingMatch();
        response.setMatches(List.of(match));

        mockWebClient(response);

        // when
        boolean result = client.checkUrl("http://malicious.com");

        // then
        assertTrue(result);
    }

    @Test
    void checkUrl_위협없음_false반환() {
        // given
        SafeBrowsingResponse response = new SafeBrowsingResponse(); // matches = null
        mockWebClient(response);

        // when
        boolean result = client.checkUrl("http://safe.com");

        // then
        assertFalse(result);
    }

    @Test
    void checkUrl_에러응답_예외발생() {
        // given
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeaders(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(requestSpec);
        when(requestSpec.uri(anyString())).thenReturn(bodySpec);

        @SuppressWarnings("unchecked")
        WebClient.RequestHeadersSpec<?> casted = headersSpec;
        when(bodySpec.bodyValue(any())).thenAnswer(invocation -> casted);

        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SafeBrowsingResponse.class))
                .thenThrow(new RuntimeException("API 실패"));

        // when & then
        assertThrows(CustomException.class, () -> client.checkUrl("http://error.com"));
    }

    private void mockWebClient(SafeBrowsingResponse mockResponse) {
        when(webClientBuilder.baseUrl(anyString())).thenReturn(webClientBuilder);
        when(webClientBuilder.defaultHeaders(any())).thenReturn(webClientBuilder);
        when(webClientBuilder.build()).thenReturn(webClient);

        when(webClient.post()).thenReturn(requestSpec);
        when(requestSpec.uri(anyString())).thenReturn(bodySpec);

        @SuppressWarnings("unchecked")
        WebClient.RequestHeadersSpec<?> casted = headersSpec;
        when(bodySpec.bodyValue(any())).thenAnswer(invocation -> casted);

        when(headersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(SafeBrowsingResponse.class)).thenReturn(Mono.just(mockResponse));
    }
}
