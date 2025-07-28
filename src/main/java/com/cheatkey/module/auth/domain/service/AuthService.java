package com.cheatkey.module.auth.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.service.token.RefreshTokenService;
import com.cheatkey.module.auth.domain.validate.NicknameValidator;
import com.cheatkey.module.auth.interfaces.dto.SignInResponse;
import com.cheatkey.module.terms.domain.service.TermsAgreementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final TermsAgreementService termsAgreementService;

    private final AuthRepository authRepository;
    private final NicknameValidator nicknameValidator;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;


    public void validateNickname(String nickname) {
        nicknameValidator.checkFormat(nickname);

        if (authRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.AUTH_DUPLICATE_NICKNAME);
        }
    }

    /**
     * 이미 가입된 사용자인지 확인. 가입된 경우 예외 발생
     */
    public void checkNotRegistered(Long userId) {
        Optional<Auth> auth = authRepository.findById(userId);

        if(auth.isPresent()) {
            if(!auth.get().getStatus().equals(AuthStatus.PENDING)) {
                throw new CustomException(ErrorCode.AUTH_ALREADY_REGISTERED);
            }
        } else {
            throw new CustomException(ErrorCode.AUTH_NOT_FOUND);
        }
    }

    public String getNickname(Long userId) {
        Auth auth = authRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_NOT_FOUND));
        
        String nickname = auth.getNickname();
        if (nickname == null || nickname.trim().isEmpty()) {
            throw new CustomException(ErrorCode.AUTH_NOT_FOUND);
        }
        
        return nickname;
    }

    @Transactional
    public void register(Auth updateInfo, Long userId, List<Long> requiredIds, List<Long> optionalIds) {
        Auth auth = authRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));

        // 이미 가입된 사용자(상태 ACTIVE 등)라면 예외
        if (auth.getStatus() == AuthStatus.ACTIVE) {
            throw new CustomException(ErrorCode.AUTH_ALREADY_REGISTERED);
        }

        // 닉네임 중복 체크 (본인 닉네임은 허용)
        if (!updateInfo.getNickname().equals(auth.getNickname()) &&
            authRepository.existsByNickname(updateInfo.getNickname())) {
            throw new CustomException(ErrorCode.AUTH_DUPLICATE_NICKNAME);
        }

        // 필수 정보 체크
        if (updateInfo.getAgeCode() == null || updateInfo.getGenderCode() == null) {
            throw new CustomException(ErrorCode.AUTH_MISSING_REQUIRED_INFORMATION);
        }

        nicknameValidator.checkFormat(updateInfo.getNickname());

        // 기존 Auth 엔티티에 정보 업데이트
        auth.setNickname(updateInfo.getNickname());
        auth.setAgeCode(updateInfo.getAgeCode());
        auth.setGenderCode(updateInfo.getGenderCode());
        auth.setTradeMethodCodes(updateInfo.getTradeMethodCodes());
        auth.setTradeItemCodes(updateInfo.getTradeItemCodes());
        auth.setStatus(AuthStatus.ACTIVE); // 가입 완료로 상태 변경

        authRepository.save(auth);

        // 약관 동의 처리
        termsAgreementService.processAgreement(auth, requiredIds, optionalIds);
    }

    public SignInResponse refreshAccessToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        Long userId = Long.valueOf(jwtProvider.getUserIdFromToken(refreshToken));
        Auth auth = authRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));

        // 새로운 액세스 토큰과 리프레시 토큰 생성
        String accessJwt = jwtProvider.createAccessToken(auth.getId(), auth.getProvider(), auth.getRole());
        String newRefreshJwt = jwtProvider.createRefreshToken(auth.getId());

        // 기존 리프레시 토큰 무효화하고 새로운 토큰 저장
        refreshTokenService.invalidateToken(refreshToken, userId);
        refreshTokenService.saveOrUpdate(userId, newRefreshJwt);

        return SignInResponse.builder()
                .userState(auth.getStatus().name())
                .grantType("Bearer")
                .accessToken(accessJwt)
                .refreshToken(newRefreshJwt)
                .build();
    }

    public void deleteUser(Long userId) {
        Auth auth = authRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));
        auth.setStatus(AuthStatus.WITHDRAWN);
        authRepository.save(auth);
    }
}
