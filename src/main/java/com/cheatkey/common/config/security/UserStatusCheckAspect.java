package com.cheatkey.common.config.security;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class UserStatusCheckAspect {
    private final AuthRepository authRepository;

    @Before("within(@org.springframework.web.bind.annotation.RestController *) && !@annotation(com.cheatkey.common.config.security.SkipUserStatusCheck)")
    public void checkUserStatus() {
        String userId = com.cheatkey.common.config.security.SecurityUtil.getCurrentUserId();
        Auth auth = authRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));
        if (auth.getStatus() != AuthStatus.ACTIVE) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
    }
} 