package com.cheatkey.module.auth.domain.service;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.AuthLoginHistory;
import com.cheatkey.module.auth.domain.repository.AuthLoginHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthLoginHistoryService {
    private final AuthLoginHistoryRepository authLoginHistoryRepository;

    public void recordLogin(Auth auth, String ipAddress, String userAgent, boolean success, String failReason) {
        AuthLoginHistory history = AuthLoginHistory.builder()
                .auth(auth)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .success(success)
                .failReason(failReason)
                .build();
        authLoginHistoryRepository.save(history);
    }
} 