package com.cheatkey.module.mypage.domain.mapper;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.ProfileImage;
import com.cheatkey.module.community.domain.entity.CommunityPost;
import com.cheatkey.module.mypage.interfaces.dto.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class MyPageMapper {

    public UserInfoResponse toUserInfoResponse(Auth auth, ProfileImage profileImage, Integer totalVisitCount) {
        return UserInfoResponse.builder()
                .nickname(auth.getNickname())
                .totalVisitCount(totalVisitCount)
                .level(1) // 고정 레벨 1
                .profileImageId(auth.getProfileImageId())
                .profileImageUrl(profileImage != null ? profileImage.getImageUrl() : null)
                .profileImageName(profileImage != null ? profileImage.getImageName() : null)
                .build();
    }

    public ProfileImageResponse toProfileImageResponse(ProfileImage profileImage) {
        return ProfileImageResponse.builder()
                .id(profileImage.getId())
                .imageUrl(profileImage.getImageUrl())
                .imageName(profileImage.getImageName())
                .build();
    }

    public List<ProfileImageResponse> toProfileImageResponseList(List<ProfileImage> profileImages) {
        return profileImages.stream()
                .map(this::toProfileImageResponse)
                .collect(Collectors.toList());
    }

    public UserPostResponse toUserPostResponse(CommunityPost post, Long commentCount, List<String> imageUrls) {
        return UserPostResponse.builder()
                .id(post.getId())
                .nickname(post.getNickname())
                .title(post.getTitle())
                .content(post.getContent())
                .createdAt(post.getCreatedAt())
                .commentCount(commentCount)
                .imageUrls(imageUrls)
                .build();
    }
} 