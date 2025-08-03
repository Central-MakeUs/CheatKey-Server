package com.cheatkey.module.auth.domain.service.token;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.token.RefreshToken;
import com.cheatkey.module.auth.domain.repository.token.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;

    public void saveOrUpdate(Long userId, String token) {
        refreshTokenRepository.save(
            RefreshToken.builder().userId(userId).token(token).build()
        );
    }

    public void invalidateToken(String token, Long userId) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
            .orElseThrow(() -> new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
        if (!refreshToken.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        refreshTokenRepository.deleteByToken(token);
    }

    public boolean existsByToken(String token) {
        return refreshTokenRepository.findByToken(token).isPresent();
    }

    public void invalidateTokenByUserId(Long userId) {
        refreshTokenRepository.deleteById(userId);
    }
} 