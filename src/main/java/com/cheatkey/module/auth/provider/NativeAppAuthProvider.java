package com.cheatkey.module.auth.provider;

import com.cheatkey.module.auth.interfaces.dto.AppUserDetails;
import com.cheatkey.module.auth.interfaces.dto.AppUserDetailsRequest;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor
public class NativeAppAuthProvider implements AuthenticationProvider {

    private final AuthRepository authRepository;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Long kakaoId = (Long) authentication.getPrincipal();
        log.info("authenticate authentication principal: {}", authentication.getPrincipal());

        Auth auth = authRepository.findByKakaoId(kakaoId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + kakaoId));

        log.info("authenticate auth kakaoId: {}", auth.getKakaoId());

        AppUserDetailsRequest appUserDetailsRequest = AppUserDetailsRequest.from(auth);

        return new UsernamePasswordAuthenticationToken(
                new AppUserDetails(appUserDetailsRequest), // 인증된 사용자 정보
                null,
                new AppUserDetails(appUserDetailsRequest).getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
