package com.cheatkey.module.auth.domain.service.kakao;

import com.cheatkey.module.auth.interfaces.dto.kakao.KakaoUserResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class KakaoUserInfoService {
    private final WebClient webClient = WebClient.create("https://kapi.kakao.com");

    public String fetchEmail(String accessToken) {
        KakaoUserResponse response = webClient.get()
                .uri("/v2/user/me")
                .header("Authorization", "Bearer " + accessToken)
                .retrieve()
                .bodyToMono(KakaoUserResponse.class)
                .block();

        if (response != null && response.kakaoAccount != null) {
            return response.kakaoAccount.email;
        }
        return null;
    }
}