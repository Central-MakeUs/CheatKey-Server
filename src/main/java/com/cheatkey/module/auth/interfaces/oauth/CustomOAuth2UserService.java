package com.cheatkey.module.auth.interfaces.oauth;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.interfaces.oauth.dto.CustomOAuth2User;
import com.cheatkey.module.auth.interfaces.dto.OAuthUserRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AuthRepository authRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 카카오에서 사용자 정보 요청
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        log.debug("OAuth2User attributes: {}", attributes);

        // 카카오 고유 사용자 ID 추출
        Long kakaoId = Long.valueOf(attributes.get("id").toString());

        // 사용자 정보 DB 조회 or 신규 생성
        Auth auth = authRepository.findByKakaoId(kakaoId)
                .orElseGet(() -> {
                    log.info("신규 카카오 사용자: {}", kakaoId);
                    return Auth.builder()
                            .kakaoId(kakaoId)
                            .status(AuthStatus.PENDING)
                            .build();
                });

        authRepository.save(auth); // 신규 가입자일 경우 저장

        OAuthUserRequest customUserRequest = OAuthUserRequest.builder()
                .kakaoId(auth.getKakaoId())
                .authStatus(auth.getStatus())
                .build();

        return new CustomOAuth2User(customUserRequest);
    }
}
