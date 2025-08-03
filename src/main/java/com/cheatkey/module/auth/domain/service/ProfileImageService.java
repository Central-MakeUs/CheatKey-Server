package com.cheatkey.module.auth.domain.service;

import com.cheatkey.module.auth.domain.entity.ProfileImage;
import com.cheatkey.module.auth.domain.repository.ProfileImageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileImageService {

    private final ProfileImageRepository profileImageRepository;

    /**
     * 프로필 기본 이미지 목록 조회
     */
    public List<ProfileImage> getProfileImages() {
        return profileImageRepository.findAllActiveOrderByDisplayOrder();
    }

    /**
     * 프로필 이미지 URL 조회
     */
    public String getProfileImageUrl(Long profileImageId) {
        if (profileImageId == null) {
            return null;
        }
        
        ProfileImage profileImage = profileImageRepository.findByIdAndActive(profileImageId);
        return profileImage != null ? profileImage.getImageUrl() : null;
    }

    /**
     * 기본 프로필 이미지 URL 조회
     */
    public String getDefaultProfileImageUrl() {
        ProfileImage defaultImage = profileImageRepository.findByIdAndActive(1L);
        return defaultImage != null ? defaultImage.getImageUrl() : null;
    }

    /**
     * 작성자 프로필 이미지 URL 조회 (NULL인 경우 기본 이미지 사용)
     */
    public String getAuthorProfileImageUrl(Long authorProfileImageId) {
        if (authorProfileImageId == null) {
            return getDefaultProfileImageUrl();
        }
        
        ProfileImage profileImage = profileImageRepository.findByIdAndActive(authorProfileImageId);
        return profileImage != null ? profileImage.getImageUrl() : getDefaultProfileImageUrl();
    }
} 