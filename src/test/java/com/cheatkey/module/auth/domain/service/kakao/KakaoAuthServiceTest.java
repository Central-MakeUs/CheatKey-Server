package com.cheatkey.module.auth.domain.service.kakao;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class KakaoAuthServiceTest {

    @InjectMocks
    private KakaoAuthService kakaoAuthService;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    @BeforeEach
    void setUp() {
        given(request.getSession()).willReturn(session);
        ReflectionTestUtils.setField(kakaoAuthService, "redirectUri", "http://localhost:3000/api/auth/login/kakao/callback");
    }

    @Test
    void 카카오_로그인_성공시_kakaoId를_세션에_저장하고_반환한다() throws Exception {
        // given
        String code = "dummy-auth-code";
        String accessToken = "dummy-access-token";
        Long expectedKakaoId = 123456789L;

        // 1단계: 액세스 토큰 요청 응답
        ResponseEntity<String> tokenResponse = new ResponseEntity<>(
                "{\"access_token\": \"" + accessToken + "\"}",
                HttpStatus.OK
        );
        given(restTemplate.postForEntity(anyString(), any(), eq(String.class))).willReturn(tokenResponse);

        // 2단계: 사용자 정보 요청 응답
        ResponseEntity<String> userInfoResponse = new ResponseEntity<>(
                "{\"id\": " + expectedKakaoId + "}",
                HttpStatus.OK
        );
        given(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(), eq(String.class))).willReturn(userInfoResponse);

        // JSON 파싱 mocking
        JsonNode tokenNode = mock(JsonNode.class);
        JsonNode userNode = mock(JsonNode.class);

        JsonNode idNode = mock(JsonNode.class);
        given(idNode.isNumber()).willReturn(true);
        given(idNode.asLong()).willReturn(expectedKakaoId);

        JsonNode tokenAccessNode = mock(JsonNode.class);
        given(tokenAccessNode.asText()).willReturn(accessToken);

        // mocking
        given(objectMapper.readTree(tokenResponse.getBody())).willReturn(tokenNode);
        given(tokenNode.get("access_token")).willReturn(tokenAccessNode);

        given(objectMapper.readTree(userInfoResponse.getBody())).willReturn(userNode);
        given(userNode.get("id")).willReturn(idNode);

        // when
        Long kakaoId = kakaoAuthService.handleKakaoLogin(code, request);

        // then
        assertEquals(expectedKakaoId, kakaoId);
        verify(session).setAttribute("loginUser", expectedKakaoId);
    }
}

