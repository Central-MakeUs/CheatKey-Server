package com.cheatkey.module.auth.domain.repository;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.Provider;
import com.cheatkey.module.auth.domain.entity.AuthStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Long> {
    boolean existsByNickname(String nickname);

    // 기존 메서드 - 탈퇴된 사용자도 포함하여 조회 (중복 데이터가 있을 수 있음)
    Optional<Auth> findByProviderAndProviderId(Provider provider, String providerId);
    
    // 새로운 메서드 - 탈퇴된 사용자를 제외하고 활성/대기 상태의 사용자만 조회
    @Query("SELECT a FROM Auth a WHERE a.provider = :provider AND a.providerId = :providerId AND a.status != 'WITHDRAWN'")
    Optional<Auth> findActiveByProviderAndProviderId(@Param("provider") Provider provider, @Param("providerId") String providerId);
    
    /**
     * 탈퇴한 사용자 ID 목록 조회
     */
    @Query("SELECT a.id FROM Auth a WHERE a.status = :status")
    List<Long> findIdsByStatus(@Param("status") AuthStatus status);
}
