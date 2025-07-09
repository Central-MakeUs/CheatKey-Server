package com.cheatkey.module.auth.interfaces.controller;

import com.cheatkey.common.handler.oauth.KakaoAuthenticationSuccessHandler;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.interfaces.dto.AuthRegisterRequest;
import com.cheatkey.module.terms.domain.entity.AuthTermsAgreement;
import com.cheatkey.module.terms.domain.entity.Terms;
import com.cheatkey.module.terms.domain.repository.AuthTermsAgreementRepository;
import com.cheatkey.module.terms.domain.repository.TermsRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

    @Autowired
    private TermsRepository termsRepository;

    @Autowired
    private AuthTermsAgreementRepository authTermsAgreementRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void 회원가입_초기_데이터_조회_성공() throws Exception {
        // given
        Long mockKakaoId = 999999L;

        Map<String, Object> attributes = Map.of("kakaoId", mockKakaoId);
        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "kakaoId"
        );

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(oauth2User, null, oauth2User.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // when & then
        mockMvc.perform(get("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ageCodeList").isArray())
                .andExpect(jsonPath("$.genderCodeList").isArray())
                .andExpect(jsonPath("$.tradeMethodCodeList").isArray())
                .andExpect(jsonPath("$.tradeItemCodeList").isArray())
                .andExpect(jsonPath("$.termsList").isArray());
    }

    @Test
    public void 회원가입_성공() throws Exception {
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

        // 운영 약관 ID 기준 하드코딩 (변경 주의)
        request.setAgreedRequiredTerms(List.of(1L, 2L));
        request.setAgreedOptionalTerms(List.of(3L));

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

        List<AuthTermsAgreement> agreements = authTermsAgreementRepository.findAllByAuth(saved.get());
        assertThat(agreements).hasSize(3);
        assertThat(agreements).extracting(a -> a.getTerms().getId())
                .containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    public void 로그인_성공시_로그인횟수_증가_및_최근로그인시간_업데이트() throws Exception {
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
    public void 로그아웃_성공() throws Exception {
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk());
    }
}
