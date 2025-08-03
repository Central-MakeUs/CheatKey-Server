package com.cheatkey.module.auth.interfaces.controller;

import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthRole;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.service.AuthService;
import com.cheatkey.module.auth.domain.service.AuthSignInService;
import com.cheatkey.module.auth.domain.service.token.RefreshTokenService;
import com.cheatkey.module.auth.domain.mapper.AuthMapper;
import com.cheatkey.common.code.domain.service.CodeService;
import com.cheatkey.module.terms.domain.service.TermsService;
import com.cheatkey.module.terms.domain.mapper.TermsMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthSignInService authSignInService;

    @MockBean
    private JwtProvider jwtProvider;

    @MockBean
    private AuthRepository authRepository;

    @MockBean
    private RefreshTokenService refreshTokenService;

    @MockBean
    private AuthService authService;

    @MockBean
    private AuthMapper authMapper;

    @MockBean
    private CodeService codeService;

    @MockBean
    private TermsService termsService;

    @MockBean
    private TermsMapper termsMapper;

    @Test
    @DisplayName("정상 소셜 로그인 요청시 JWT가 발급된다")
    void socialLogin_success() throws Exception {
        Auth mockAuth = Auth.builder()
                .id(1L)
                .provider(Provider.KAKAO)
                .providerId("mockProviderId")
                .email("test@kakao.com")
                .status(AuthStatus.PENDING)
                .role(AuthRole.USER)
                .build();

        given(authSignInService.signIn(any(), any(), any(), any(), any()))
                .willReturn(mockAuth);
        given(jwtProvider.createAccessToken(anyLong(), any(), any())).willReturn("mockAccessTokenJwt");
        given(jwtProvider.createRefreshToken(anyLong(), any())).willReturn("mockRefreshTokenJwt");

        Map<String, Object> req = new HashMap<>();
        req.put("provider", "KAKAO");
        req.put("idToken", "mockIdToken");
        req.put("accessToken", "mockAccessToken");

        mockMvc.perform(post("/v1/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mockAccessTokenJwt"))
                .andExpect(jsonPath("$.refreshToken").value("mockRefreshTokenJwt"))
                .andExpect(jsonPath("$.userState").value(AuthStatus.PENDING.name()));
    }

    @Test
    @DisplayName("provider가 잘못된 경우 400 반환")
    void socialLogin_invalidProvider() throws Exception {
        Map<String, Object> req = new HashMap<>();
        req.put("provider", "INVALID");
        req.put("idToken", "mockIdToken");
        req.put("accessToken", "mockAccessToken");

        mockMvc.perform(post("/v1/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("로그인/회원가입 등 @SkipUserStatusCheck 적용 API는 상태와 무관하게 정상 동작")
    void skipUserStatusCheckApi_worksRegardlessOfStatus() throws Exception {
        // GIVEN: WITHDRAWN 상태의 유저
        Long userId = 102L;
        Auth withdrawnAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.WITHDRAWN)
                .role(AuthRole.USER)
                .build();

        given(authSignInService.signIn(any(), any(), any(), any(), any()))
                .willReturn(withdrawnAuth);
        given(jwtProvider.createAccessToken(anyLong(), any(), any())).willReturn("mockAccessTokenJwt");
        given(jwtProvider.createRefreshToken(anyLong(), any())).willReturn("mockRefreshTokenJwt");

        Map<String, Object> req = new HashMap<>();
        req.put("provider", "KAKAO");
        req.put("idToken", "mockIdToken");
        req.put("accessToken", "mockAccessToken");

        // WHEN & THEN: 로그인은 상태와 무관하게 정상 동작
        mockMvc.perform(post("/v1/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mockAccessTokenJwt"))
                .andExpect(jsonPath("$.refreshToken").value("mockRefreshTokenJwt"));
    }

    @Test
    @DisplayName("리프레시 토큰으로 액세스 토큰 재발급 성공")
    @WithMockUser(username = "103", authorities = "ROLE_USER")
    void refreshToken_success() throws Exception {
        // GIVEN
        String refreshToken = "mockRefreshToken";
        Long userId = 103L;
        Auth activeAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();

        given(refreshTokenService.existsByToken(refreshToken)).willReturn(true);
        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(jwtProvider.getRoleFromToken(anyString())).willReturn("ROLE_USER");
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(activeAuth));
        given(jwtProvider.createAccessToken(anyLong(), any(), any())).willReturn("newAccessToken");
        given(jwtProvider.createRefreshToken(anyLong(), any())).willReturn("newRefreshToken");
        doNothing().when(refreshTokenService).invalidateToken(anyString(), anyLong());
        doNothing().when(refreshTokenService).saveOrUpdate(anyLong(), anyString());

        // AuthService.refreshAccessToken Mock
        com.cheatkey.module.auth.interfaces.dto.SignInResponse mockResponse = 
            com.cheatkey.module.auth.interfaces.dto.SignInResponse.builder()
                .userState("ACTIVE")
                .grantType("Bearer")
                .accessToken("newAccessToken")
                .refreshToken("newRefreshToken")
                .build();
        given(authService.refreshAccessToken(anyString())).willReturn(mockResponse);

        // WHEN & THEN - 토큰 순환 패턴: 새로운 액세스 토큰과 리프레시 토큰 모두 생성
        mockMvc.perform(post("/v1/api/auth/refresh")
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("newAccessToken"))
                .andExpect(jsonPath("$.refreshToken").value("newRefreshToken")); // 새로운 리프레시 토큰
    }

    @Test
    @DisplayName("존재하지 않는 리프레시 토큰으로 재발급 요청시 404 반환")
    @WithMockUser(username = "999", authorities = "ROLE_USER")
    void refreshToken_notFound() throws Exception {
        // GIVEN
        String refreshToken = "invalidRefreshToken";
        Long userId = 999L;
        Auth activeAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();

        given(refreshTokenService.existsByToken(refreshToken)).willReturn(false);
        // Auth 조회까지 가지 않지만, 보안상 일관성을 위해 Mock 설정
        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(jwtProvider.getRoleFromToken(anyString())).willReturn("ROLE_USER");
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(activeAuth));

        // WHEN & THEN - refreshTokenService.existsByToken이 false를 반환하면 404 반환
        mockMvc.perform(post("/v1/api/auth/refresh")
                .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isNotFound()); // 404 Not Found
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    @WithMockUser(username = "104", authorities = "ROLE_USER")
    void withdraw_success() throws Exception {
        // GIVEN
        Long userId = 104L;
        Auth activeAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();

        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(jwtProvider.getRoleFromToken(anyString())).willReturn("ROLE_USER");
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(activeAuth));
        doNothing().when(refreshTokenService).invalidateTokenByUserId(userId);

        // WHEN & THEN
        mockMvc.perform(delete("/v1/api/auth/withdraw")
                .header("Authorization", "Bearer mockToken"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("로그아웃 성공")
    @WithMockUser(username = "105", authorities = "ROLE_USER")
    void logout_success() throws Exception {
        // GIVEN
        Long userId = 105L;
        Auth activeAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();

        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(jwtProvider.getRoleFromToken(anyString())).willReturn("ROLE_USER");
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(activeAuth));
        doNothing().when(refreshTokenService).invalidateToken(anyString(), anyLong());

        // WHEN & THEN
        mockMvc.perform(post("/v1/api/auth/logout")
                .header("Authorization", "Bearer mockRefreshToken"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("회원가입 초기 정보 조회 성공")
    @WithMockUser(username = "201", authorities = "ROLE_USER")
    void initRegister_success() throws Exception {
        // GIVEN - PENDING 상태의 사용자 (소셜 로그인 후 아직 가입하지 않은 상태)
        Long userId = 201L;
        Auth pendingAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.PENDING)
                .role(AuthRole.USER)
                .build();

        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(jwtProvider.getRoleFromToken(anyString())).willReturn("ROLE_USER");
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(pendingAuth));

        // Mock CodeService and TermsService
        given(codeService.getOptionsByType(any())).willReturn(List.of());
        given(termsService.getTermsForRegistration()).willReturn(List.of());
        given(termsMapper.toDtoList(any())).willReturn(List.of());

        // WHEN & THEN
        mockMvc.perform(get("/v1/api/auth/register")
                .header("Authorization", "Bearer mockToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ageCodeList").exists())
                .andExpect(jsonPath("$.genderCodeList").exists())
                .andExpect(jsonPath("$.tradeMethodCodeList").exists())
                .andExpect(jsonPath("$.tradeItemCodeList").exists())
                .andExpect(jsonPath("$.termsList").exists());
    }

    @Test
    @DisplayName("PENDING 상태 사용자의 회원가입 초기 정보 조회 성공")
    @WithMockUser(username = "202", authorities = "ROLE_USER")
    void initRegister_pendingUser_success() throws Exception {
        Long userId = 202L;
        Auth pendingAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.PENDING)
                .role(AuthRole.USER)
                .build();

        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(jwtProvider.getRoleFromToken(anyString())).willReturn("ROLE_USER");
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(pendingAuth));

        // WHEN & THEN
        mockMvc.perform(get("/v1/api/auth/register")
                .header("Authorization", "Bearer mockToken"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ageCodeList").exists())
                .andExpect(jsonPath("$.genderCodeList").exists())
                .andExpect(jsonPath("$.tradeMethodCodeList").exists())
                .andExpect(jsonPath("$.tradeItemCodeList").exists())
                .andExpect(jsonPath("$.termsList").exists());
    }

    @Test
    @DisplayName("닉네임 중복 체크 성공")
    @WithMockUser(username = "107", authorities = "ROLE_USER")
    void checkNickname_success() throws Exception {
        // GIVEN
        Long userId = 107L;
        Auth pendingAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.PENDING)
                .role(AuthRole.USER)
                .build();

        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(jwtProvider.getRoleFromToken(anyString())).willReturn("ROLE_USER");
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(pendingAuth));

        // WHEN & THEN
        mockMvc.perform(get("/v1/api/auth/register/nickname-check")
                .param("nickname", "테스트트트")
                .header("Authorization", "Bearer mockToken"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("닉네임 중복 체크 실패")
    @WithMockUser(username = "108", authorities = "ROLE_USER")
    void checkNickname_duplicate() throws Exception {
        // GIVEN
        Long userId = 108L;
        Auth pendingAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.PENDING)
                .role(AuthRole.USER)
                .build();

        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(jwtProvider.getRoleFromToken(anyString())).willReturn("ROLE_USER");
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(pendingAuth));
        given(authRepository.existsByNickname("중복닉네임")).willReturn(true);
        doThrow(new com.cheatkey.common.exception.CustomException(com.cheatkey.common.exception.ErrorCode.AUTH_DUPLICATE_NICKNAME))
                .when(authService).validateNickname("중복닉네임");

        // WHEN & THEN
        mockMvc.perform(get("/v1/api/auth/register/nickname-check")
                .param("nickname", "중복닉네임")
                .header("Authorization", "Bearer mockToken"))
                .andExpect(status().isConflict()); // 409 Conflict
    }

    @Test
    @DisplayName("회원가입 완료 성공")
    @WithMockUser(username = "109", authorities = "ROLE_USER")
    void register_success() throws Exception {
        // GIVEN
        Long userId = 109L;
        Auth pendingAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.PENDING)
                .role(AuthRole.USER)
                .build();

        Auth mappedAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .nickname("테스트12")
                .ageCode("20")
                .genderCode("M")
                .tradeMethodCode("CASH,BANK")
                .tradeItemCode("ELECTRONICS")
                .build();

        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(jwtProvider.getRoleFromToken(anyString())).willReturn("ROLE_USER");
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(pendingAuth));
        given(authMapper.toAuth(any())).willReturn(mappedAuth);
        doNothing().when(refreshTokenService).saveOrUpdate(anyLong(), anyString());
        given(authService.register(any(), anyLong(), any(), any())).willReturn(mappedAuth);

        Map<String, Object> req = new HashMap<>();
        req.put("nickname", "테스트12");
        req.put("ageCode", "20");
        req.put("genderCode", "M");
        req.put("tradeMethodCodeList", java.util.List.of("CASH", "BANK"));
        req.put("tradeItemCodeList", java.util.List.of("ELECTRONICS"));
        req.put("agreedRequiredTerms", java.util.List.of(1L, 2L));
        req.put("agreedOptionalTerms", java.util.List.of(3L));

        // WHEN & THEN
        mockMvc.perform(post("/v1/api/auth/register")
                .header("Authorization", "Bearer mockToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userState").value("ACTIVE"));
    }

    @Test
    @DisplayName("이미 가입된 사용자의 회원가입 시도 실패")
    @WithMockUser(username = "110", authorities = "ROLE_USER")
    void register_alreadyRegistered() throws Exception {
        // GIVEN
        Long userId = 110L;
        Auth activeAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .build();

        Auth mappedAuth = Auth.builder()
                .id(userId)
                .provider(Provider.KAKAO)
                .status(AuthStatus.ACTIVE)
                .role(AuthRole.USER)
                .nickname("테스트12")
                .ageCode("20")
                .genderCode("M")
                .build();

        given(jwtProvider.validateToken(anyString())).willReturn(true);
        given(jwtProvider.getUserIdFromToken(anyString())).willReturn(userId.toString());
        given(jwtProvider.getRoleFromToken(anyString())).willReturn("ROLE_USER");
        given(authRepository.findById(userId)).willReturn(java.util.Optional.of(activeAuth));
        given(authMapper.toAuth(any())).willReturn(mappedAuth);
        doThrow(new com.cheatkey.common.exception.CustomException(com.cheatkey.common.exception.ErrorCode.AUTH_ALREADY_REGISTERED))
                .when(authService).register(any(), anyLong(), any(), any());

        // 완전한 요청 데이터 (400 에러 방지)
        Map<String, Object> req = new HashMap<>();
        req.put("nickname", "테스트12");
        req.put("ageCode", "20");
        req.put("genderCode", "M");
        req.put("tradeMethodCodeList", java.util.List.of("CASH"));
        req.put("tradeItemCodeList", java.util.List.of("ELECTRONICS"));
        req.put("agreedRequiredTerms", java.util.List.of(1L, 2L));
        req.put("agreedOptionalTerms", java.util.List.of(3L));

        // WHEN & THEN
        mockMvc.perform(post("/v1/api/auth/register")
                .header("Authorization", "Bearer mockToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict()); // 409 Conflict
    }
} 