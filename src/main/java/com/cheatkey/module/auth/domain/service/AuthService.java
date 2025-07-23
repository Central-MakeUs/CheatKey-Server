package com.cheatkey.module.auth.domain.service;

import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.validate.NicknameValidator;
import com.cheatkey.module.terms.domain.service.TermsAgreementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final TermsAgreementService termsAgreementService;

    private final AuthRepository authRepository;
    private final NicknameValidator nicknameValidator;


    public void validateNickname(String nickname) {
        nicknameValidator.checkFormat(nickname);

        if (authRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.AUTH_DUPLICATE_NICKNAME);
        }
    }

    @Transactional
    public void register(Auth auth, Long kakaoId, List<Long> requiredIds, List<Long> optionalIds) {

//        if (authRepository.findByKakaoId(kakaoId).isPresent()) {
//            throw new CustomException(ErrorCode.AUTH_ALREADY_REGISTERED);
//        }

        if (authRepository.existsByNickname(auth.getNickname())) {
            throw new CustomException(ErrorCode.AUTH_DUPLICATE_NICKNAME);
        }

        if (auth.getAgeCode() == null || auth.getGenderCode() == null) {
            throw new CustomException(ErrorCode.AUTH_MISSING_REQUIRED_INFORMATION);
        }

        nicknameValidator.checkFormat(auth.getNickname());

//        auth.assignKakaoId(kakaoId);
        auth.increaseLoginCount();
        auth.updateLastLoginTime(LocalDateTime.now());

        authRepository.save(auth);

        termsAgreementService.processAgreement(auth, requiredIds, optionalIds);
    }
}
