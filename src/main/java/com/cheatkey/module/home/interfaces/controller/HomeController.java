package com.cheatkey.module.home.interfaces.controller;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.home.interfaces.dto.HomeInitResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/home")
@Tag(name = "Home", description = "홈화면(메인) API")
public class HomeController {

    private final AuthRepository authRepository;

    @Operation(summary = "홈화면(메인)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "홈 초기 정보 반환"),
            @ApiResponse(responseCode = "401", description = "로그인이 필요한 요청입니다.")
    })
    @GetMapping()
    public ResponseEntity<HomeInitResponse> home(HttpServletRequest request) {
        Object kakaoIdAttr = request.getSession().getAttribute("loginUser");

        if (!(kakaoIdAttr instanceof Long kakaoId)) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        Auth auth = authRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_NOT_FOUND));

        boolean welcome = Boolean.TRUE.equals(request.getSession().getAttribute("welcome"));
        if (welcome) {
            request.getSession().removeAttribute("welcome");
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
