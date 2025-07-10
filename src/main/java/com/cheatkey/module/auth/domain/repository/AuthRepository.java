package com.cheatkey.module.auth.domain.repository;

import com.cheatkey.module.auth.domain.entity.Auth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Long> {
    Optional<Auth> findByKakaoId(Long kakaoId);

    boolean existsByNickname(String nickname);

    void deleteByKakaoId(long kakaoId);
}
