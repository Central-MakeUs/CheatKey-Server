package com.cheatkey.module.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "auth_login_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuthLoginHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_id", nullable = false)
    private Auth auth;

    private LocalDateTime loginAt;

    private String ipAddress;

    private Boolean success;

    public static AuthLoginHistory success(Auth auth, String ipAddress) {
        return AuthLoginHistory.builder()
                .auth(auth)
                .loginAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .success(true)
                .build();
    }

    public static AuthLoginHistory fail(Auth auth, String ipAddress) {
        return AuthLoginHistory.builder()
                .auth(auth)
                .loginAt(LocalDateTime.now())
                .ipAddress(ipAddress)
                .success(false)
                .build();
    }
}
