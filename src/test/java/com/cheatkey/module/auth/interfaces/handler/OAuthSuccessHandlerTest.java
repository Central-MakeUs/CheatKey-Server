package com.cheatkey.module.auth.interfaces.handler;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.jwt.JwtUtil;
import com.cheatkey.module.auth.domain.service.RefreshManager;
import com.cheatkey.module.auth.interfaces.oauth.dto.CustomOAuth2User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthSuccessHandlerTest {

    @InjectMocks
    private OAuthSuccessHandler oAuthSuccessHandler;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private RefreshManager refreshManager;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setup() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        oAuthSuccessHandler = new OAuthSuccessHandler(jwtUtil, refreshManager);
    }

    @Test
    void 성공적으로_엑세스_토큰과_리프레시_토큰을_발급하고_쿠키_리다이렉트를_설정한다() throws Exception {
        // given
        Long kakaoId = 12345L;
        String accessToken = "mock-access-token";
        String refreshToken = "mock-refresh-token";

        CustomOAuth2User principal = mock(CustomOAuth2User.class);
        given(principal.getKakaoId()).willReturn(kakaoId);

        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null);

        given(jwtUtil.createJwt("access", kakaoId, 604800000L)).willReturn(accessToken);
        given(jwtUtil.createJwt("refresh", kakaoId, 86400000L)).willReturn(refreshToken);

        request.addHeader("Referer", "http://localhost");

        // when
        oAuthSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        assertThat(response.getHeader("Authorization")).isEqualTo("Bearer " + accessToken);
        assertThat(response.getCookies()).anyMatch(cookie ->
                cookie.getName().equals("refreshToken") &&
                        cookie.getValue().equals(refreshToken) &&
                        cookie.getMaxAge() > 0
        );

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/signup?accessToken=" + accessToken + "&refreshToken=" + refreshToken);

        then(refreshManager).should().addRefreshEntity(kakaoId, refreshToken, 86400000L);
    }

    @Test
    void OAuth2_인증이_아닌_경우_예외_발생() {
        // given
        Authentication authentication = mock(Authentication.class);
        given(authentication.getPrincipal()).willReturn("NotOAuthUser"); // 잘못된 타입

        // when & then
        assertThrows(CustomException.class, () -> {
            oAuthSuccessHandler.onAuthenticationSuccess(request, response, authentication);
        });
    }
}
