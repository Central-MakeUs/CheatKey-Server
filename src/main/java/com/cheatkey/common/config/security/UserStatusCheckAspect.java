package com.cheatkey.common.config.security;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class UserStatusCheckAspect {
    private final AuthRepository authRepository;

    @Before("within(@org.springframework.web.bind.annotation.RestController *) && !@annotation(com.cheatkey.common.config.security.SkipUserStatusCheck)")
    public void checkUserStatus(JoinPoint joinPoint) {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String requestURI = request.getRequestURI();
            
            // Swagger 관련 경로는 체크하지 않음
            if (isSwaggerRequest(requestURI)) {
                return;
            }
        }
        
        // 사용자 상태 체크 로직
        String userId = SecurityUtil.getCurrentUserId();
        Auth auth = authRepository.findById(Long.valueOf(userId))
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));
        
        if (auth.getStatus() != AuthStatus.ACTIVE) {
            log.warn("비활성 사용자 접근 시도: userId={}, status={}", userId, auth.getStatus());
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
    }
    
    /**
     * Swagger 관련 요청인지 확인
     */
    private boolean isSwaggerRequest(String requestURI) {
        return requestURI.startsWith("/swagger-ui") || 
               requestURI.startsWith("/v3/api-docs") || 
               requestURI.startsWith("/swagger-ui.html");
    }
} 