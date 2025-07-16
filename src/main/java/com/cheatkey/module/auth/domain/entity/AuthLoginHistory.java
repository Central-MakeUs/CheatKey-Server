package com.cheatkey.module.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "t_auth_login_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuthLoginHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_id", nullable = false)
    private Auth auth;

    private LocalDateTime loginAt;
    private String ipAddress;
    private String userAgent;
    private Boolean success;
    private String failReason;

    @PrePersist
    public void prePersist() {
        if (loginAt == null) loginAt = LocalDateTime.now();
    }
}
