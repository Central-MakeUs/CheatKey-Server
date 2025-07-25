package com.cheatkey.module.auth.interfaces.controller;

import com.cheatkey.common.code.domain.entity.CodeType;
import com.cheatkey.common.code.domain.service.CodeService;
import com.cheatkey.common.config.security.SecurityUtil;
import com.cheatkey.common.config.security.SkipUserStatusCheck;
import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.mapper.AuthMapper;
import com.cheatkey.module.auth.domain.service.AuthService;
import com.cheatkey.module.auth.domain.service.AuthSignInService;
import com.cheatkey.module.auth.domain.service.token.RefreshTokenService;
import com.cheatkey.module.auth.interfaces.dto.*;
import com.cheatkey.module.terms.domain.entity.Terms;
import com.cheatkey.module.terms.domain.mapper.TermsMapper;
import com.cheatkey.module.terms.domain.service.TermsService;
import com.cheatkey.module.terms.interfaces.dto.TermsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static com.cheatkey.common.code.interfaces.dto.OptionsResponse.Option;

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/api/auth")
@Tag(name = "Auth", description = "로그인 및 회원가입 관련 API")
public class AuthController {

    private final AuthService authService;
    private final TermsService termsService;
    private final AuthSignInService authSignInService;
    private final CodeService codeService;
    private final RefreshTokenService refreshTokenService;
    
    private final AuthMapper authMapper;
    private final TermsMapper termsMapper;
    private final JwtProvider jwtProvider;

    @Operation(summary = "(★) 소셜 로그인", description = "provider, idToken, accessToken(카카오만)을 받아 JWT를 발급합니다. 신규 사용자는 자동 회원가입.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공, JWT 반환", content = @Content(schema = @Schema(implementation = SignInResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청/토큰 오류")
    })
    @SkipUserStatusCheck
    @PostMapping("/login")
    public ResponseEntity<?> socialLogin(HttpServletRequest request, @Valid @RequestBody SocialLoginRequest socialLoginRequest) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        Provider provider;

        try {
            provider = Provider.valueOf(socialLoginRequest.getProvider().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_PROVIDER);
        }

        Auth auth = authSignInService.signIn(provider, socialLoginRequest.getIdToken(), socialLoginRequest.getAccessToken(), ip, userAgent);
        if (auth == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        String accessJwt = jwtProvider.createAccessToken(auth.getId(), auth.getProvider(), auth.getRole());
        String refreshJwt = jwtProvider.createRefreshToken(auth.getId());

        refreshTokenService.saveOrUpdate(auth.getId(), refreshJwt);
        return ResponseEntity.ok(SignInResponse.builder()
                .userState(auth.getStatus().name())
                .grantType("Bearer")
                .accessToken(accessJwt)
                .refreshToken(refreshJwt)
                .build());
    }

    @Operation(summary = "회원가입 초기 정보 조회", description = "로그인된 사용자만 접근할 수 있으며, 이미 가입된 사용자인 경우 예외를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 정보 반환"),
            @ApiResponse(responseCode = "401", description = "로그인되지 않은 상태"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 사용자")
    })
    @SkipUserStatusCheck
    @GetMapping("/register")
    public ResponseEntity<AuthRegisterInitResponse> initRegister() {
        String userId = com.cheatkey.common.config.security.SecurityUtil.getCurrentUserId();
        Long loginId = Long.valueOf(userId);
        authService.checkNotRegistered(loginId);

        List<Option> ageCodeList = codeService.getOptionsByType(CodeType.AGE_GROUP);
        List<Option> genderCodeList = codeService.getOptionsByType(CodeType.GENDER);
        List<Option> tradeMethodCodeList = codeService.getOptionsByType(CodeType.TRADE_METHOD);
        List<Option> tradeItemCodeList = codeService.getOptionsByType(CodeType.TRADE_ITEM);

        List<Terms> terms = termsService.getTermsForRegistration();
        List<TermsDto> termsList = termsMapper.toDtoList(terms);

        AuthRegisterInitResponse response = AuthRegisterInitResponse.builder()
                .ageCodeList(ageCodeList)
                .genderCodeList(genderCodeList)
                .tradeMethodCodeList(tradeMethodCodeList)
                .tradeItemCodeList(tradeItemCodeList)
                .termsList(termsList)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "(★) 닉네임 중복 체크", description = "입력한 닉네임이 이미 사용 중인지 검증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용 가능한 닉네임"),
            @ApiResponse(responseCode = "409", description = "중복된 닉네임")
    })
    @SkipUserStatusCheck
    @GetMapping("/register/nickname-check")
    public ResponseEntity<Void> checkNickname(@Parameter(description = "중복 체크할 닉네임", example = "테스트")
                                              @RequestParam @NotBlank String nickname) {
        authService.validateNickname(nickname);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원가입", description = "로그인된 사용자만 접근 가능하며, 사용자 정보를 등록하고 회원가입을 완료합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "로그인되지 않은 상태"),
            @ApiResponse(responseCode = "409", description = "중복된 정보 등으로 실패")
    })
    @SkipUserStatusCheck
    @PostMapping("/register")
    public ResponseEntity<SignInResponse> register(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "회원가입 요청 정보")
                                         @RequestBody @Valid AuthRegisterRequest registerRequest) {
        String userId = SecurityUtil.getCurrentUserId();
        Long loginId = Long.valueOf(userId);

        Auth requestAuth = authMapper.toAuth(registerRequest);
        authService.register(requestAuth, loginId, registerRequest.getAgreedRequiredTerms(), registerRequest.getAgreedOptionalTerms());

        // 회원가입 후 바로 토큰 발급 및 저장 (자동 로그인)
        String accessJwt = jwtProvider.createAccessToken(loginId, requestAuth.getProvider(), requestAuth.getRole());
        String refreshJwt = jwtProvider.createRefreshToken(loginId);
        refreshTokenService.saveOrUpdate(loginId, refreshJwt);

        SignInResponse response = SignInResponse.builder()
                .userState(requestAuth.getStatus().name())
                .grantType("Bearer")
                .accessToken(accessJwt)
                .refreshToken(refreshJwt)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "로그아웃", description = "로그인된 사용자의 로그아웃 처리. 리프레시 토큰 무효화")
    @ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        String userId = SecurityUtil.getCurrentUserId();
        refreshTokenService.invalidateToken(request.getRefreshToken(), Long.valueOf(userId));
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원 탈퇴", description = "로그인된 사용자의 회원 정보를 삭제하고, 리프레시 토큰을 무효화합니다.")
    @ApiResponse(responseCode = "200", description = "회원 탈퇴 성공")
    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdraw() {
        String userId = SecurityUtil.getCurrentUserId();
        Long loginId = Long.valueOf(userId);

        authService.deleteUser(loginId);
        refreshTokenService.invalidateTokenByUserId(loginId);

        return ResponseEntity.ok().build();
    }

    @Operation(summary = "(★) 리프레시 토큰으로 액세스 토큰 재발급", description = "리프레시 토큰을 받아 새로운 액세스 토큰(및 필요시 리프레시 토큰)을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공, JWT 반환", content = @Content(schema = @Schema(implementation = SignInResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청/토큰 오류")
    })
    @PostMapping("/refresh")
    public ResponseEntity<SignInResponse> refresh(@RequestBody RefreshTokenRequest request) {
        if (!refreshTokenService.existsByToken(request.getRefreshToken())) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        SignInResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}
