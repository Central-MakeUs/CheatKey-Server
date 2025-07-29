package com.cheatkey.module.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_user_activity")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class UserActivity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @JoinColumn(name = "auth_id", nullable = false)
    private Auth auth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType activityType;

    private String ipAddress;
    private String userAgent;
    private Boolean success;
    private String failReason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum ActivityType {
        SOCIAL_LOGIN,      // 실제 소셜 로그인
        TOKEN_REFRESH,     // 토큰 갱신 (자동 로그인)
        HOME_VISIT,        // 메인 대시보드 방문
        MYPAGE_VISIT       // 마이페이지 방문
    }
} 