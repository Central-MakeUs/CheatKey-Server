package com.cheatkey.module.auth.domain.repository;

import com.cheatkey.module.auth.domain.entity.AuthLoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthLoginHistoryRepository extends JpaRepository<AuthLoginHistory, Long> {
} 