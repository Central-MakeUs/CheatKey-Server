package com.cheatkey.module.auth.domain.repository;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface AuthLoginHistoryRepository extends JpaRepository<AuthLoginHistory, Long> {
    
    @Query("SELECT COUNT(h) FROM AuthLoginHistory h WHERE h.auth = :auth AND h.loginAt BETWEEN :startDate AND :endDate AND h.success = :success")
    long countByAuthAndLoginAtBetweenAndSuccess(@Param("auth") Auth auth, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, @Param("success") boolean success);
} 