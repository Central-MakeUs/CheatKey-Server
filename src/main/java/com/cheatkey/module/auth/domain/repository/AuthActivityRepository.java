package com.cheatkey.module.auth.domain.repository;

import com.cheatkey.module.auth.domain.entity.AuthActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface AuthActivityRepository extends JpaRepository<AuthActivity, Long> {

    @Query("SELECT COUNT(ua) FROM AuthActivity ua WHERE ua.auth.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
} 