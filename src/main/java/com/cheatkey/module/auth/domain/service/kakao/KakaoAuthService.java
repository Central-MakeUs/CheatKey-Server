package com.cheatkey.module.auth.domain.service.kakao;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAuthService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kakao.client-id}")
    private String clientId;

    @Value("${app.kakao.redirect-uri}")
    private String redirectUri;

    public Long handleKakaoLogin(String code, HttpServletRequest request)  throws IOException {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId);
        params.add("redirect_uri", redirectUri);
        params.add("code", code);

        HttpEntity<?> tokenRequest = new HttpEntity<>(params, headers);
        ResponseEntity<String> tokenResponse = restTemplate.postForEntity(
                "https://kauth.kakao.com/oauth/token", tokenRequest, String.class);

        if (!tokenResponse.getStatusCode().is2xxSuccessful()) {
            throw new CustomException(ErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
        }

        JsonNode accessTokenNode = objectMapper.readTree(tokenResponse.getBody()).get("access_token");
        if (accessTokenNode == null) {
            throw new CustomException(ErrorCode.KAKAO_TOKEN_REQUEST_FAILED);
        }
        String accessToken = accessTokenNode.asText();

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);

        HttpEntity<?> userInfoRequest = new HttpEntity<>(authHeaders);
        ResponseEntity<String> userInfoResponse = restTemplate.exchange(
                "https://kapi.kakao.com/v2/user/me", HttpMethod.GET, userInfoRequest, String.class);

        JsonNode userJson = objectMapper.readTree(userInfoResponse.getBody());
        JsonNode idNode = userJson.get("id");
        if (idNode == null || !idNode.isNumber()) {
            throw new CustomException(ErrorCode.AUTH_NOT_FOUND);
        }
        Long kakaoId = idNode.asLong();
        request.getSession().setAttribute("loginUser", kakaoId);

        return kakaoId;
    }
}
