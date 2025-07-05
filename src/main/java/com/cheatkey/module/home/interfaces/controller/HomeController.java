package com.cheatkey.module.home.interfaces.controller;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.home.interfaces.dto.HomeInitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Tag(name = "Home", description = "홈화면(메인) API")
public class HomeController {

    private final AuthRepository authRepository;

    @Operation(summary = "홈화면(메인)")
    @GetMapping("/home")
    public ResponseEntity<HomeInitResponse> home(@AuthenticationPrincipal OAuth2User oauth2User,
                                                 HttpSession session) {
        Long kakaoId = oauth2User.getAttribute("kakaoId");

        Auth auth = authRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_NOT_FOUND));

        boolean welcome = Boolean.TRUE.equals(session.getAttribute("welcome"));
        if (welcome) {
            session.removeAttribute("welcome");
        }

        HomeInitResponse response = new HomeInitResponse(
                welcome,
                auth.getNickname(),
                auth.getLoginCount() >= 1,
                auth.getTradeMethodCodes(),
                auth.getTradeItemCodes()
        );

        return ResponseEntity.ok(response);
    }
}
