package com.cheatkey.module.home.application.facade;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.service.AuthService;
import com.cheatkey.module.auth.domain.service.ProfileImageService;
import com.cheatkey.module.community.domian.entity.CommunityPost;
import com.cheatkey.module.community.domian.service.CommunityService;
import com.cheatkey.module.home.domain.mapper.HomeMapper;
import com.cheatkey.module.home.interfaces.dto.HomeDashboardResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeFacade {

    private final AuthService authService;
    private final CommunityService communityService;
    private final ProfileImageService profileImageService;
    private final HomeMapper homeMapper;

    public HomeDashboardResponse getDashboard(Long userId) {
        try {
            Auth userInfo = authService.getUserInfo(userId);
            // TODO: 페이징 처리로 변경 검토 중 (현재는 성능상 limit 처리 유지)
            List<CommunityPost> popularPosts = communityService.getPopularPosts(10);
            
            HomeDashboardResponse.UserInfo userInfoDto = homeMapper.toUserInfo(userInfo);
            List<HomeDashboardResponse.PopularPost> popularPostDtos = homeMapper.toPopularPosts(popularPosts);

            // 이미지 URL 설정
            String profileImageUrl = profileImageService.getProfileImageUrl(userInfo.getProfileImageId());
            userInfoDto = HomeDashboardResponse.UserInfo.builder()
                    .profileImageUrl(profileImageUrl)
                    .level(userInfoDto.getLevel())
                    .nickname(userInfoDto.getNickname())
                    .totalVisitCount(userInfoDto.getTotalVisitCount())
                    .build();

            // 인기글은 닉네임, 제목, 내용만 노출하므로 추가 처리 불필요

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