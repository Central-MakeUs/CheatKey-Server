package com.cheatkey.module.auth.domain.service;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.UserActivity;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.repository.UserActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserActivityServiceTest {

    @Mock
    private UserActivityRepository userActivityRepository;

    @Mock
    private AuthRepository authRepository;

    @InjectMocks
    private UserActivityService userActivityService;

    private Auth mockAuth;

    @BeforeEach
    void setUp() {
        mockAuth = Auth.builder()
                .id(1L)
                .nickname("테스트")
                .totalVisitCount(0)
                .build();
    }

    @Test
    void recordActivity_소셜로그인_성공() {
        // given
        Long userId = 1L;
        String ipAddress = "127.0.0.1";
        String userAgent = "Mozilla/5.0";
        when(authRepository.findById(userId)).thenReturn(Optional.of(mockAuth));
        when(userActivityRepository.save(any(UserActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        userActivityService.recordActivity(userId, UserActivity.ActivityType.SOCIAL_LOGIN, ipAddress, userAgent, true, null);

        // then
        verify(authRepository).findById(userId);
        verify(userActivityRepository).save(any(UserActivity.class));
    }

    @Test
    void recordActivity_토큰갱신_성공() {
        // given
        Long userId = 1L;
        when(authRepository.findById(userId)).thenReturn(Optional.of(mockAuth));
        when(userActivityRepository.save(any(UserActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        userActivityService.recordActivity(userId, UserActivity.ActivityType.TOKEN_REFRESH, null, null, true, null);

        // then
        verify(authRepository).findById(userId);
        verify(userActivityRepository).save(any(UserActivity.class));
    }

    @Test
    void recordDashboardVisit_메인대시보드_하루에한번만_카운팅() {
        // given
        Long userId = 1L;
        when(authRepository.findById(userId)).thenReturn(Optional.of(mockAuth));
        when(userActivityRepository.save(any(UserActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when - 첫 번째 방문
        userActivityService.recordDashboardVisit(userId, UserActivity.ActivityType.HOME_VISIT);

        // then
        verify(authRepository, times(1)).save(any(Auth.class));
        verify(userActivityRepository).save(any(UserActivity.class));

        // when - 두 번째 방문 (같은 날)
        userActivityService.recordDashboardVisit(userId, UserActivity.ActivityType.HOME_VISIT);

        // then - 방문 횟수는 증가하지 않음
        verify(authRepository, times(1)).save(any(Auth.class));
        verify(userActivityRepository, times(2)).save(any(UserActivity.class));
    }

    @Test
    void recordDashboardVisit_마이페이지_하루에한번만_카운팅() {
        // given
        Long userId = 1L;
        when(authRepository.findById(userId)).thenReturn(Optional.of(mockAuth));
        when(userActivityRepository.save(any(UserActivity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when - 첫 번째 방문
        userActivityService.recordDashboardVisit(userId, UserActivity.ActivityType.MYPAGE_VISIT);

        // then
        verify(authRepository, times(1)).save(any(Auth.class));
        verify(userActivityRepository).save(any(UserActivity.class));

        // when - 두 번째 방문 (같은 날)
        userActivityService.recordDashboardVisit(userId, UserActivity.ActivityType.MYPAGE_VISIT);

        // then - 방문 횟수는 증가하지 않음
        verify(authRepository, times(1)).save(any(Auth.class));
        verify(userActivityRepository, times(2)).save(any(UserActivity.class));
    }

    @Test
    void recordActivity_사용자없음_예외발생() {
        // given
        Long userId = 999L;
        when(authRepository.findById(userId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(RuntimeException.class, () -> {
            userActivityService.recordActivity(userId, UserActivity.ActivityType.SOCIAL_LOGIN, null, null, true, null);
        });
    }
} 