package com.cheatkey.module.auth.interfaces.controller;

import com.cheatkey.common.code.domain.entity.CodeType;
import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.util.RedirectResolver;
import com.cheatkey.common.util.SecurityUtil;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.mapper.AuthMapper;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.service.AuthService;
import com.cheatkey.module.auth.domain.service.kakao.KakaoAuthService;
import com.cheatkey.module.auth.interfaces.dto.AuthInfoOptionsResponse.Option;
import com.cheatkey.module.auth.interfaces.dto.AuthRegisterInitResponse;
import com.cheatkey.module.auth.interfaces.dto.AuthRegisterRequest;
import com.cheatkey.module.terms.domain.entity.Terms;
import com.cheatkey.module.terms.domain.mapper.TermsMapper;
import com.cheatkey.module.terms.domain.service.TermsService;
import com.cheatkey.module.terms.interfaces.dto.TermsDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth")
@Tag(name = "(★)Auth", description = "로그인 및 회원가입 관련 API")
public class AuthController {

    private final AuthService authService;
    private final KakaoAuthService kakaoAuthService;
    private final TermsService termsService;

    private final AuthRepository authRepository;

    private final AuthMapper authMapper;
    private final TermsMapper termsMapper;

    private final RedirectResolver redirectResolver;

    @Operation(summary = "(★)카카오 로그인 콜백", description = "JS SDK 로그인 완료 후 인가코드를 처리하고, 세션에 사용자 정보를 저장합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "302", description = "로그인 성공 후 프론트로 리다이렉트 (/signup 또는 /home)")
    })
    @GetMapping("/login/kakao/callback")
    public void kakaoCallback(@RequestParam("code") String code,
                              HttpServletResponse response, HttpServletRequest request) throws IOException {

        Long kakaoId = kakaoAuthService.handleKakaoLogin(code, request);
        String baseRedirect = redirectResolver.resolveRedirectBase(request);

        if(authRepository.findByKakaoId(kakaoId).isEmpty()) {
            response.sendRedirect(baseRedirect + "/signup");
        } else {
            response.sendRedirect(baseRedirect + "/home");
        }
    }

    @Operation(summary = "(★)로그인 상태 확인", description = "세션에 로그인된 사용자의 kakaoId를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인된 사용자"),
            @ApiResponse(responseCode = "401", description = "로그인되지 않은 상태")
    })
    @GetMapping("/me")
    public ResponseEntity<?> me(HttpServletRequest request) {
        Object kakaoId = SecurityUtil.getLoginUserId(request);
        if (kakaoId == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return ResponseEntity.ok(Map.of("loginUser", kakaoId));
    }

    @Operation(summary = "(★)회원가입 초기 정보 조회", description = "세션에 로그인된 사용자만 접근할 수 있으며, 이미 가입된 사용자인 경우 예외를 반환합니다.")
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

        if (authRepository.findByKakaoId(id).isPresent()) {
            throw new CustomException(ErrorCode.AUTH_ALREADY_REGISTERED);
        }

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

    @Operation(summary = "(★)닉네임 중복 체크", description = "세션에 로그인된 사용자만 사용할 수 있으며, 입력한 닉네임이 이미 사용 중인지 검증합니다.")
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

    @Operation(summary = "(★)회원가입", description = "세션에 로그인된 사용자만 접근 가능하며, 사용자 정보를 등록하고 회원가입을 완료합니다.")
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

    @Operation(summary = "(★)로그아웃", description = "서버 세션을 무효화합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "401", description = "이미 로그인되어 있지 않음")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);

        if (session == null || session.getAttribute("loginUser") == null) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        session.invalidate();
        return ResponseEntity.noContent().build();
    }
}
