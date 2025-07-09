package com.cheatkey.module.detection.domain.service;

import com.cheatkey.module.detection.domain.entity.DetectionInput;
import com.cheatkey.module.detection.domain.entity.DetectionResult;
import com.cheatkey.module.detection.domain.entity.DetectionStatus;
import com.cheatkey.module.detection.domain.entity.DetectionType;
import com.cheatkey.module.detection.domain.repository.DetectionHistoryRepository;
import com.cheatkey.module.detection.infra.client.UrlDetectionClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.BDDMockito.given;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class UrlDetectionServiceTest {

    @InjectMocks
    private UrlDetectionService urlDetectionService;

    @Mock
    private UrlDetectionClient urlDetectionClient;

    @Mock
    private DetectionHistoryRepository detectionHistoryRepository;

    @Test
    public void 위험한_URL_감지시_DANGER_반환() {
        // given
        Long kakaoId = 99999L;

        OAuth2User mockUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("kakaoId", kakaoId),
                "kakaoId"
        );

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities())
        );

        String url = "http://malicious.com";
        DetectionInput input = new DetectionInput(url, DetectionType.URL);
        given(urlDetectionClient.checkUrl(url)).willReturn(true);

        // when
        DetectionResult result = urlDetectionService.detect(input);

        // then
        assertEquals(DetectionStatus.DANGER, result.status());
    }
}