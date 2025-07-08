package com.cheatkey.module.auth.interfaces.controller;

import com.cheatkey.common.handler.oauth.KakaoAuthenticationSuccessHandler;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.interfaces.dto.AuthRegisterRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@Transactional
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private KakaoAuthenticationSuccessHandler successHandler;

    @Autowired
    private AuthRepository authRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 회원가입_성공() throws Exception {
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

        AuthRegisterRequest request = new AuthRegisterRequest();
        request.setNickname("테스터");
        request.setAgeCode("30_40");
        request.setGenderCode("FEMALE");
        request.setTradeMethodCodeList(List.of("SNS", "APP"));
        request.setTradeItemCodeList(List.of("FASHION", "LUXURY"));

        // when
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // then
        Optional<Auth> saved = authRepository.findByKakaoId(kakaoId);
        assertThat(saved).isPresent();
        assertThat(saved.get().getNickname()).isEqualTo("테스터");
        assertThat(saved.get().getTradeMethodCodes()).containsExactlyInAnyOrder("SNS", "APP");
    }


    @Test
    void 로그인_성공시_로그인횟수_증가_및_최근로그인시간_업데이트() throws Exception {
        // given: 기존 가입된 사용자
        Long kakaoId = 99999L;
        Auth saved = authRepository.save(Auth.builder()
                .kakaoId(kakaoId)
                .nickname("테스터")
                .ageCode("30_40")
                .genderCode("FEMALE")
                .loginCount(3)
                .lastLoginAt(LocalDateTime.now().minusDays(1))
                .build());

        authRepository.flush();

        // OAuth2User mock 구성
        Map<String, Object> kakaoAttributes = Map.of("kakaoId", kakaoId);
        DefaultOAuth2User user = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                kakaoAttributes,
                "kakaoId"
        );
        OAuth2AuthenticationToken authToken = new OAuth2AuthenticationToken(
                user, user.getAuthorities(), "kakao"
        );

        // when: 성공 핸들러 직접 호출
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        successHandler.onAuthenticationSuccess(request, response, authToken);

        // then: DB에서 다시 조회
        Auth updated = authRepository.findByKakaoId(kakaoId).orElseThrow();

        assertThat(updated.getLoginCount()).isEqualTo(4); // 기존 3 → 4
        assertThat(updated.getLastLoginAt()).isNotNull();
        assertThat(response.getRedirectedUrl()).isEqualTo("/home");
    }

    @Test
    @WithMockUser
    void 로그아웃_성공() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk());
    }
}
