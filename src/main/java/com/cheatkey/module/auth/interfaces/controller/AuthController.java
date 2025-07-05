package com.cheatkey.module.auth.interfaces.controller;

import com.cheatkey.common.code.domain.entity.CodeType;
import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.mapper.AuthMapper;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.interfaces.dto.AuthInfoOptionsResponse.Option;
import com.cheatkey.module.auth.domain.service.AuthService;
import com.cheatkey.module.auth.interfaces.dto.AuthRegisterInitResponse;
import com.cheatkey.module.auth.interfaces.dto.AuthRegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "로그인 및 회원가입 관련 API")
public class AuthController {

    private final AuthService authService;
    private final AuthRepository authRepository;
    private final AuthMapper authMapper;

    @Operation(
            summary = "카카오 로그인 트리거",
            description = "카카오 로그인 인증 플로우를 시작합니다. 302 응답으로 리디렉션됩니다.",
            responses = {@ApiResponse(responseCode = "302", description = "카카오 로그인 페이지로 리디렉션")}
    )
    @GetMapping("/login")
    public ResponseEntity<Void> loginTrigger() {
        URI kakaoLoginUri = URI.create("/oauth2/authorization/kakao");
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(kakaoLoginUri)
                .build();
    }

    @Operation(summary = "회원가입 초기 정보 조회", description = "카카오 ID로 이미 가입된 회원인지 확인하고, 초기 가입 정보를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 정보 반환"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 사용자")
    })
    @GetMapping("/register")
    public ResponseEntity<AuthRegisterInitResponse> initRegister() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication.getPrincipal() instanceof OAuth2User)) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED); // 또는 401 Unauthorized
        }

        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();
        Long kakaoId = oauth2User.getAttribute("kakaoId");

        if (authRepository.findByKakaoId(kakaoId).isPresent()) {
            throw new CustomException(ErrorCode.AUTH_ALREADY_REGISTERED);
        }

        List<Option> ageCodeList = authService.getOptionsByType(CodeType.AGE_GROUP);
        List<Option> genderCodeList = authService.getOptionsByType(CodeType.GENDER);
        List<Option> tradeMethodCodeList = authService.getOptionsByType(CodeType.TRADE_METHOD);
        List<Option> tradeItemCodeList = authService.getOptionsByType(CodeType.TRADE_ITEM);

        AuthRegisterInitResponse response = AuthRegisterInitResponse.builder()
                .ageCodeList(ageCodeList)
                .genderCodeList(genderCodeList)
                .tradeMethodCodeList(tradeMethodCodeList)
                .tradeItemCodeList(tradeItemCodeList)
                .build();

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "닉네임 중복 체크", description = "입력한 닉네임이 이미 사용 중인지 검증합니다.")
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

    @Operation(summary = "회원가입", description = "사용자 정보를 등록하고 회원가입을 완료합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원가입 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "409", description = "중복된 정보 등으로 실패")
    })
    @PostMapping("/register")
    public ResponseEntity<Void> register(@io.swagger.v3.oas.annotations.parameters.RequestBody(description = "회원가입 요청 정보")
                                         @RequestBody @Valid AuthRegisterRequest request,
                                         HttpServletRequest servletRequest) {

        OAuth2User oauth2User = (OAuth2User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long kakaoId = oauth2User.getAttribute("kakaoId");

        Auth requestAuth = authMapper.toAuth(request);
        authService.register(requestAuth, kakaoId);

        servletRequest.getSession().setAttribute("welcome", true);

        return ResponseEntity.ok().build();
    }
}
