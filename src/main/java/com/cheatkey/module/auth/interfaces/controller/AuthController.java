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
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final AuthRepository authRepository;
    private final AuthMapper authMapper;

    @GetMapping("/login")
    public ResponseEntity<Void> loginPageRedirect() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, "/")
                .build();
    }

    @GetMapping("/register")
    public ResponseEntity<AuthRegisterInitResponse> initRegister(@RequestParam ("kakaoId") Long kakaoId) {
        if (authRepository.findByKakaoId(kakaoId).isPresent()) {
            throw new CustomException(ErrorCode.AUTH_ALREADY_REGISTERED);
        }

        AuthRegisterInitResponse response = authService.getRegisterInitInfo(kakaoId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/register/nickname-check")
    public ResponseEntity<Void> checkNickname(@RequestParam @NotBlank String nickname) {
        authService.validateNickname(nickname);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/register/options/age")
    public List<Option> getAgeOptions() {
        return authService.getOptionsByType(CodeType.AGE_GROUP);
    }

    @GetMapping("/register/options/gender")
    public List<Option> getGenderOptions() {
        return authService.getOptionsByType(CodeType.GENDER);
    }

    @GetMapping("/register/options/trade-method")
    public List<Option> getTradeMethodOptions() {
        return authService.getOptionsByType(CodeType.TRADE_METHOD);
    }

    @GetMapping("/register/options/trade-item")
    public List<Option> getTradeItemOptions() {
        return authService.getOptionsByType(CodeType.TRADE_ITEM);
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid AuthRegisterRequest request,
                                         @RequestAttribute("kakaoId") Long kakaoId) {

        Auth requestAuth = authMapper.toAuth(request);
        authService.register(requestAuth, kakaoId);

        return ResponseEntity.ok().build();
    }
}
