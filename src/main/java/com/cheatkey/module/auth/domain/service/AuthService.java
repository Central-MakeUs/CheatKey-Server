package com.cheatkey.module.auth.domain.service;

import com.cheatkey.common.code.domain.entity.CodeType;
import com.cheatkey.common.exception.CustomException;
import com.cheatkey.common.exception.ErrorCode;
import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.interfaces.dto.AuthInfoOptionsResponse;
import com.cheatkey.module.auth.interfaces.dto.AuthInfoOptionsResponse.Option;
import com.cheatkey.common.code.domain.repository.CodeRepository;
import com.cheatkey.module.auth.domain.repository.AuthRepository;
import com.cheatkey.module.auth.domain.validate.NicknameValidator;
import com.cheatkey.module.auth.interfaces.dto.AuthRegisterInitResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthRepository authRepository;
    private final CodeRepository codeRepository;
    private final NicknameValidator nicknameValidator;


    public void validateNickname(String nickname) {
        nicknameValidator.checkFormat(nickname);

        if (authRepository.existsByNickname(nickname)) {
            throw new CustomException(ErrorCode.AUTH_DUPLICATE_NICKNAME);
        }
    }

    public AuthRegisterInitResponse getRegisterInitInfo(Long kakaoId) {
        OAuth2User oauth2User = (OAuth2User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();

        Map<String, Object> kakaoAccount = (Map<String, Object>) oauth2User.getAttribute("kakao_account");
        String kakaoName = null;

        // @TODO 이름(닉네임) 확인 필요
        if (kakaoAccount != null && kakaoAccount.containsKey("profile")) {
            Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
            kakaoName = profile.get("nickname").toString();
        }

        return new AuthRegisterInitResponse(kakaoId, kakaoName);
    }

    public List<Option> getOptionsByType(CodeType type) {
        return AuthInfoOptionsResponse.from(codeRepository.findAllByType(type));
    }

    @Transactional
    public void register(Auth auth, Long kakaoId) {

        if (authRepository.findByKakaoId(kakaoId).isPresent()) {
            throw new CustomException(ErrorCode.AUTH_ALREADY_REGISTERED);
        }

        if (authRepository.existsByNickname(auth.getNickname())) {
            throw new CustomException(ErrorCode.AUTH_DUPLICATE_NICKNAME);
        }

        if (auth.getAgeCode() == null || auth.getGenderCode() == null) {
            throw new CustomException(ErrorCode.AUTH_MISSING_REQUIRED_INFORMATION);
        }

        nicknameValidator.checkFormat(auth.getNickname());

        auth.assignKakaoId(kakaoId);
        auth.increaseLoginCount();
        auth.updateLastLoginTime(LocalDateTime.now());

        authRepository.save(auth);
    }
}
