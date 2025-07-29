package com.cheatkey.module.auth.domain.repository;

import com.cheatkey.module.auth.domain.entity.UserActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


public interface UserActivityRepository extends JpaRepository<UserActivity, Long> {

    @Query("SELECT COUNT(ua) FROM UserActivity ua WHERE ua.auth.id = :userId")
    Long countByUserId(@Param("userId") Long userId);
} 