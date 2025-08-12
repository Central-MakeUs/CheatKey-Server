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

    Optional<Auth> findByProviderAndProviderId(Provider provider, String providerId);
    
    /**
     * 탈퇴한 사용자 ID 목록 조회
     */
    @Query("SELECT a.id FROM Auth a WHERE a.status = :status")
    List<Long> findIdsByStatus(@Param("status") AuthStatus status);
}
