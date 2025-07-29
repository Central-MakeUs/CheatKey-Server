package com.cheatkey.module.home.application.facade;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.service.AuthService;
import com.cheatkey.module.auth.domain.service.ProfileImageService;
import com.cheatkey.module.auth.domain.service.UserActivityService;
import com.cheatkey.module.auth.domain.entity.UserActivity;
import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.domian.service.CommunityService;
import com.cheatkey.module.home.domain.mapper.HomeMapper;
import com.cheatkey.module.home.interfaces.dto.HomeDashboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeFacade {

    private final AuthService authService;
    private final CommunityService communityService;
    private final ProfileImageService profileImageService;
    private final UserActivityService userActivityService;
    private final HomeMapper homeMapper;

    @Transactional
    public HomeDashboardResponse getDashboard(Long userId) {
        try {
            // 방문 기록 저장
            userActivityService.recordDashboardVisit(userId, UserActivity.ActivityType.HOME_VISIT);
            
            Auth userInfo = authService.getUserInfo(userId);
            // TODO: 페이징 처리로 변경 검토 중 (현재는 성능상 limit 처리 유지)
            List<CommunityPost> popularPosts = communityService.getPopularPosts(10);
            
            HomeDashboardResponse.UserInfo userInfoDto = homeMapper.toUserInfo(userInfo);
            List<HomeDashboardResponse.PopularPost> popularPostDtos = homeMapper.toPopularPosts(popularPosts);

            // 실제 방문 횟수 조회
            Integer totalVisitCount = userInfo.getTotalVisitCount();

            // 이미지 URL 설정
            String profileImageUrl = profileImageService.getProfileImageUrl(userInfo.getProfileImageId());
            userInfoDto = HomeDashboardResponse.UserInfo.builder()
                    .profileImageUrl(profileImageUrl)
                    .level(userInfoDto.getLevel())
                    .nickname(userInfoDto.getNickname())
                    .totalVisitCount(totalVisitCount != null ? totalVisitCount : 0)
                    .build();

            return HomeDashboardResponse.builder()
                    .userInfo(userInfoDto)
                    .popularPosts(popularPostDtos)
                    .build();
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("메인 대시보드 조회 중 오류 발생: userId={}", userId, e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
} 