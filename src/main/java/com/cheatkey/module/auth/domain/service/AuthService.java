package com.cheatkey.module.auth.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.validate.NicknameValidator;
import com.cheatkey.module.terms.domain.service.TermsAgreementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import com.cheatkey.module.auth.interfaces.dto.SignInResponse;
import com.cheatkey.common.jwt.JwtProvider;
import lombok.Setter;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final TermsAgreementService termsAgreementService;

    private final AuthRepository authRepository;
    private final NicknameValidator nicknameValidator;
    private final JwtProvider jwtProvider;


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
        if (authRepository.findById(userId).isPresent()) {
            throw new CustomException(ErrorCode.AUTH_ALREADY_REGISTERED);
        }
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

        String accessJwt = jwtProvider.createAccessToken(auth.getId(), auth.getProvider(), auth.getRole());

        return SignInResponse.builder()
                .userState(auth.getStatus().name())
                .grantType("Bearer")
                .accessToken(accessJwt)
                .refreshToken(refreshToken)
                .build();
    }

    public void deleteUser(Long userId) {
        Auth auth = authRepository.findById(userId)
            .orElseThrow(() -> new CustomException(ErrorCode.AUTH_UNAUTHORIZED));
        auth.setStatus(AuthStatus.WITHDRAWN);
        authRepository.save(auth);
    }
}
