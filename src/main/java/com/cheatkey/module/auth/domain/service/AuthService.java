package com.cheatkey.module.auth.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.common.jwt.JwtProvider;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.entity.ProfileImage;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.repository.ProfileImageRepository;
import com.cheatkey.module.auth.domain.service.token.RefreshTokenService;
import com.cheatkey.module.auth.domain.validate.NicknameValidator;
import com.cheatkey.module.auth.interfaces.dto.SignInResponse;
import com.cheatkey.module.mypage.interfaces.dto.UpdateUserInfoRequest;
import com.cheatkey.module.terms.domain.service.TermsAgreementService;
import com.cheatkey.module.auth.domain.entity.AuthActivity;
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
    private final ProfileImageRepository profileImageRepository;
    private final NicknameValidator nicknameValidator;
    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthActivityService authActivityService;


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

    public Auth getUserInfo(Long userId) {
        return authRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_NOT_FOUND));
    }

    /**
     * 마이페이지 - 사용자 정보 수정
     */
    @Transactional
    public void updateUserInfo(Long userId, UpdateUserInfoRequest request) {
        Auth auth = authRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_NOT_FOUND));
        
        if (request.getNickname() != null) {
            // 닉네임 중복 체크 (본인 닉네임은 허용)
            if (!request.getNickname().equals(auth.getNickname()) &&
                authRepository.existsByNickname(request.getNickname())) {
                throw new CustomException(ErrorCode.AUTH_DUPLICATE_NICKNAME);
            }
            nicknameValidator.checkFormat(request.getNickname());
            auth.setNickname(request.getNickname());
        }
        
        if (request.getProfileImageId() != null) {
            ProfileImage profileImage = profileImageRepository.findByIdAndActive(request.getProfileImageId());
            if (profileImage == null) {
                throw new CustomException(ErrorCode.PROFILE_IMAGE_INVALID);
            }
            auth.setProfileImageId(request.getProfileImageId());
        }
    }

    @Transactional
    public Auth register(Auth updateInfo, Long userId, List<Long> requiredIds, List<Long> optionalIds) {
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
        auth.setLevel(1);                   // 최초 등록은 레벨 1 고정
        auth.setProfileImageId(1L);         // 최초 등록은 ID = 1 고정
        auth.setStatus(AuthStatus.ACTIVE);  // 가입 완료로 상태 변경

        Auth savedAuth = authRepository.save(auth);

        // 약관 동의 처리
        termsAgreementService.processAgreement(auth, requiredIds, optionalIds);

        return savedAuth;
    }

    public SignInResponse refreshAccessToken(String refreshToken) {
        if (!jwtProvider.validateToken(refreshToken)) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        Long userId = Long.valueOf(jwtProvider.getUserIdFromToken(refreshToken));
        Auth auth = authRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));

        // 토큰 갱신 활동 기록
        authActivityService.recordActivity(userId, AuthActivity.ActivityType.TOKEN_REFRESH, null, null, true, null);

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
