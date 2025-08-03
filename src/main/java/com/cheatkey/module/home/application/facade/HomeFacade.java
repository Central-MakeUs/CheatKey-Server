package com.cheatkey.module.home.application.facade;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthActivity;
import com.cheatkey.module.auth.domain.service.AuthService;
import com.cheatkey.module.auth.domain.service.ProfileImageService;
import com.cheatkey.module.auth.domain.service.AuthActivityService;
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
    private final AuthActivityService authActivityService;
    private final HomeMapper homeMapper;

    @Transactional
    public HomeDashboardResponse getDashboard(Long userId) {
        try {
            // 방문 기록 저장
            authActivityService.recordDashboardVisit(userId, AuthActivity.ActivityType.HOME_VISIT);
            
            Auth userInfo = authService.getUserInfo(userId);
            List<CommunityService.CommunityPostWithAuthorInfo> popularPostsWithAuthorInfo = communityService.getPopularPostsWithAuthorInfo(10);
            
            HomeDashboardResponse.UserInfo userInfoDto = homeMapper.toUserInfo(userInfo);
            List<HomeDashboardResponse.PopularPost> popularPostDtos = homeMapper.toPopularPostsWithAuthorInfo(popularPostsWithAuthorInfo);

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

            // 작성자 프로필 이미지 URL 설정
            for (int i = 0; i < popularPostDtos.size(); i++) {
                HomeDashboardResponse.PopularPost postDto = popularPostDtos.get(i);
                CommunityService.CommunityPostWithAuthorInfo postWithAuthorInfo = popularPostsWithAuthorInfo.get(i);
                
                String authorProfileImageUrl = profileImageService.getAuthorProfileImageUrl(
                    postWithAuthorInfo.getAuthor() != null ? postWithAuthorInfo.getAuthor().getProfileImageId() : null
                );
                
                popularPostDtos.set(i, HomeDashboardResponse.PopularPost.builder()
                        .id(postDto.getId())
                        .title(postDto.getTitle())
                        .content(postDto.getContent())
                        .authorNickname(postDto.getAuthorNickname())
                        .authorProfileImageUrl(authorProfileImageUrl)
                        .build());
            }

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