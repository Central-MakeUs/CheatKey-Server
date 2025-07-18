package com.cheatkey.module.auth.interfaces.controller;

import com.cheatkey.common.code.domain.entity.CodeType;
import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.mapper.AuthMapper;
import com.cheatkey.module.auth.domain.service.AuthService;
import com.cheatkey.module.auth.domain.service.AuthSignInService;
import com.cheatkey.module.auth.interfaces.dto.AuthInfoOptionsResponse.Option;
import com.cheatkey.module.auth.interfaces.dto.AuthRegisterInitResponse;
import com.cheatkey.module.auth.interfaces.dto.AuthRegisterRequest;
import com.cheatkey.module.auth.interfaces.dto.SignInResponse;
import com.cheatkey.module.auth.interfaces.dto.SocialLoginRequest;
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

@RequiredArgsConstructor
@RestController
@RequestMapping("/v1/api/auth")
@Tag(name = "Auth", description = "로그인 및 회원가입 관련 API")
public class AuthController {

    private final AuthService authService;
    private final TermsService termsService;
    private final AuthSignInService authSignInService;

    private final AuthMapper authMapper;
    private final TermsMapper termsMapper;
    private final JwtProvider jwtProvider;

    @Operation(summary = "(★) 소셜 로그인", description = "provider, idToken, accessToken(카카오만)을 받아 JWT를 발급합니다. 신규 사용자는 자동 회원가입.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공, JWT 반환", content = @Content(schema = @Schema(implementation = SignInResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 요청/토큰 오류")
    })
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
        return ResponseEntity.ok(SignInResponse.builder()
                .userState(auth.getStatus().name())
                .grantType("Bearer")
                .accessToken(accessJwt)
                .refreshToken(refreshJwt)
                .build());
    }

    @Operation(summary = "회원가입 초기 정보 조회", description = "세션에 로그인된 사용자만 접근할 수 있으며, 이미 가입된 사용자인 경우 예외를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 정보 반환"),
            @ApiResponse(responseCode = "401", description = "로그인되지 않은 상태"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 사용자")
    })
    @GetMapping("/register")
    public ResponseEntity<AuthRegisterInitResponse> initRegister(HttpServletRequest request) {
        Object kakaoId = request.getSession().getAttribute("loginUser");

        if (!(kakaoId instanceof Long id)) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

//        if (authRepository.findByKakaoId(id).isPresent()) {
//            throw new CustomException(ErrorCode.AUTH_ALREADY_REGISTERED);
//        }

        List<Option> ageCodeList = authService.getOptionsByType(CodeType.AGE_GROUP);
        List<Option> genderCodeList = authService.getOptionsByType(CodeType.GENDER);
        List<Option> tradeMethodCodeList = authService.getOptionsByType(CodeType.TRADE_METHOD);
        List<Option> tradeItemCodeList = authService.getOptionsByType(CodeType.TRADE_ITEM);

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

    @Operation(summary = "(★) 닉네임 중복 체크", description = "세션에 로그인된 사용자만 사용할 수 있으며, 입력한 닉네임이 이미 사용 중인지 검증합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "사용 가능한 닉네임"),
            @ApiResponse(responseCode = "409", description = "중복된 닉네임")
    })
    @GetMapping("/register/nickname-check")
    public ResponseEntity<Void> checkNickname(@Parameter(description = "중복 체크할 닉네임", example = "테스트")
                                              @RequestParam @NotBlank String nickname) {
        authService.validateNickname(nickname);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "회원가입", description = "세션에 로그인된 사용자만 접근 가능하며, 사용자 정보를 등록하고 회원가입을 완료합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "401", description = "로그인되지 않은 상태"),
            @ApiResponse(responseCode = "409", description = "중복된 정보 등으로 실패")
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "회원가입 요청 정보")
                                         @RequestBody @Valid AuthRegisterRequest registerRequest,
                                         HttpServletRequest request) {
        Object sessionUser = request.getSession().getAttribute("loginUser");

        if (!(sessionUser instanceof Long kakaoId)) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        Auth requestAuth = authMapper.toAuth(registerRequest);
        authService.register(requestAuth, kakaoId, registerRequest.getAgreedRequiredTerms(), registerRequest.getAgreedOptionalTerms());

        request.getSession().setAttribute("welcome", true);
        return ResponseEntity.ok().build();
    }

    //@TODO 로그아웃

    //@TODO 회원 탈퇴
}
