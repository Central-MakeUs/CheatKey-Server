package com.cheatkey.module.detection.interfaces;

import com.cheatkey.module.detection.infra.client.UrlDetectionClient;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Disabled("Google Safe Browsing 실 API 호출용 테스트 - 수동 실행 전용")
class UrlDetectionClientIntegrationTest {

    @Autowired
    private UrlDetectionClient urlDetectionClient;

    @Test
    void checkUrl_정상동작_위협없음() {
        // given
        String url = "http://example.com";

        // when
        boolean result = urlDetectionClient.checkUrl(url);

        // then
        System.out.println("Google API result: " + result);
        assertFalse(result); // example.com은 보통 안전함
    }

    @Test
    void checkUrl_정상동작_위협있음() {
        // given
        String url = "http://malware.testing.google.test/testing/malware/"; // 구글 테스트용 악성 URL

        // when
        boolean result = urlDetectionClient.checkUrl(url);

        // then
        System.out.println("Google API result: " + result);
        assertTrue(result); // 실제로 위협 탐지되어야 함
    }
}

