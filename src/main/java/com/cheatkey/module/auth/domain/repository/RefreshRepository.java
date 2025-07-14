package com.cheatkey.module.auth.domain.repository;

import com.cheatkey.module.auth.domain.entity.RefreshEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshRepository extends JpaRepository<RefreshEntity, Long> {

    boolean existsByKakaoId(Long kakaoId);

    void deleteByKakaoId(Long kakaoId);
}
