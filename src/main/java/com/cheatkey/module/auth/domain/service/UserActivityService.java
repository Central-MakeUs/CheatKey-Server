package com.cheatkey.module.auth.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.UserActivity;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.repository.UserActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserActivityService {

    private final UserActivityRepository userActivityRepository;
    private final AuthRepository authRepository;


    @Transactional
    public void recordActivity(Long userId, UserActivity.ActivityType activityType, 
                             String ipAddress, String userAgent, boolean success, String failReason) {
        try {
            Auth auth = authRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.AUTH_NOT_FOUND));
            
            UserActivity activity = UserActivity.builder()
                    .auth(auth)
                    .activityType(activityType)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .success(success)
                    .failReason(failReason)
                    .build();
            
            userActivityRepository.save(activity);
        } catch (Exception e) {
            log.error("사용자 활동 기록 저장 실패: userId={}, activityType={}", userId, activityType, e);
            throw new CustomException(ErrorCode.VISIT_RECORD_FAILED);
        }
    }

    /**
     * 대시보드 방문 기록 (하루에 한 번만 카운팅)
     */
    @Transactional
    public void recordDashboardVisit(Long userId, UserActivity.ActivityType visitType) {
        // 하루에 한 번만 카운팅
        Auth auth = authRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_NOT_FOUND));
        LocalDateTime today = LocalDateTime.now().toLocalDate().atStartOfDay();
        
        if (auth.getLastVisitDate() == null || 
            auth.getLastVisitDate().toLocalDate().isBefore(today.toLocalDate())) {
            
            auth.increaseVisitCount();
            auth.updateLastVisitDate(LocalDateTime.now());
            authRepository.save(auth);
            
            log.info("방문 횟수 증가: userId={}, totalVisitCount={}", userId, auth.getTotalVisitCount());
        }
        
        // 방문 기록은 항상 저장
        recordActivity(userId, visitType, null, null, true, null);
    }
} 