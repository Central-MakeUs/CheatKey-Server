package com.cheatkey.module.auth.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.RefreshEntity;
import com.cheatkey.module.auth.domain.repository.RefreshRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.Date;

@Component
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Transactional(readOnly = true)
public class RefreshManager {

    private final RefreshRepository refreshRepository;

    @Transactional
    public void addRefreshEntity(Long kakaoId, String refresh, Long expiration) {
        Date date = new Date(System.currentTimeMillis() + expiration);

        if (refreshRepository.existsByKakaoId(kakaoId)) {
            refreshRepository.deleteByKakaoId(kakaoId);
        }
        RefreshEntity refreshEntity = RefreshEntity.builder()
                .kakaoId(kakaoId)
                .refresh(refresh)
                .expiration(date.toString())
                .build();

        refreshRepository.save(refreshEntity);
    }

    @Transactional
    public void deleteRefreshToken(Long kakaoId) {
        if (!refreshRepository.existsByKakaoId(kakaoId)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }
        refreshRepository.deleteByKakaoId(kakaoId);
    }
}
