package com.cheatkey.module.mypage.application.facade;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.ProfileImage;
import com.cheatkey.module.auth.domain.service.AuthService;
import com.cheatkey.module.auth.domain.service.ProfileImageService;
import com.cheatkey.module.auth.domain.service.AuthActivityService;
import com.cheatkey.module.auth.domain.entity.AuthActivity;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.community.domain.service.CommunityService;
import com.cheatkey.module.detection.domain.entity.DetectionPeriod;
import com.cheatkey.module.detection.domain.service.DetectionService;
import com.cheatkey.module.mypage.domain.mapper.MyPageMapper;
import com.cheatkey.module.mypage.interfaces.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MyPageFacade {

    private final AuthService authService;
    private final ProfileImageService profileImageService;
    private final AuthActivityService authActivityService;
    private final CommunityService communityService;
    private final DetectionService detectionService;
    private final MyPageMapper myPageMapper;


    @Transactional
    public MyPageDashboardResponse getMyPageDashboard(Long userId) {
        // 방문 기록 저장
        authActivityService.recordDashboardVisit(userId, AuthActivity.ActivityType.MYPAGE_VISIT);
        
        Auth auth = authService.getUserInfo(userId);
        List<ProfileImage> profileImages = profileImageService.getProfileImages();
        
        // 실제 방문 횟수 조회
        Integer totalVisitCount = auth.getTotalVisitCount();
        
        // 프로필 이미지 조회
        ProfileImage userProfileImage = null;
        if (auth.getProfileImageId() != null) {
            userProfileImage = profileImages.stream()
                    .filter(img -> img.getId().equals(auth.getProfileImageId()))
                    .findFirst()
                    .orElse(null);
        }
        
        UserInfoResponse userInfo = myPageMapper.toUserInfoResponse(auth, userProfileImage, totalVisitCount != null ? totalVisitCount : 0);
        List<ProfileImageResponse> profileImageResponses = myPageMapper.toProfileImageResponseList(profileImages);
        
        return MyPageDashboardResponse.builder()
                .userInfo(userInfo)
                .profileImages(profileImageResponses)
                .build();
    }

    @Transactional
    public void updateUserInfo(Long userId, UpdateUserInfoRequest request) {
        authService.updateUserInfo(userId, request);
    }

    public UserPostManagementResponse getUserPostManagement(Long userId, Pageable pageable) {
        Page<CommunityPost> postPage = communityService.getUserPosts(userId, pageable);
        
        if (postPage.isEmpty()) {
            return new UserPostManagementResponse(pageable.getPageNumber(), pageable.getPageSize(), List.of(), 0);
        }
        
        List<Long> postIds = postPage.getContent().stream().map(CommunityPost::getId).toList();
        Map<Long, Integer> commentCountMap = communityService.getCommentCountMap(postIds);
        Map<Long, List<String>> imageUrlsMap = communityService.getPostImageUrlsMap(postIds);
        
        List<UserPostResponse> userPosts = postPage.getContent().stream()
                .map(post -> {
                    Long commentCount = commentCountMap.getOrDefault(post.getId(), 0).longValue();
                    List<String> imageUrls = imageUrlsMap.getOrDefault(post.getId(), List.of());
                    return myPageMapper.toUserPostResponse(post, commentCount, imageUrls);
                })
                .collect(Collectors.toList());
        
        return new UserPostManagementResponse(
                postPage.getNumber(),
                postPage.getSize(),
                userPosts,
                postPage.getTotalElements()
        );
    }

    public Page<DetectionHistoryResponse> getDetectionHistory(Long userId, String period, Pageable pageable) {
        DetectionPeriod detectionPeriod = DetectionPeriod.fromValue(period);
        return detectionService.getDetectionHistory(userId, detectionPeriod, pageable);
    }
}