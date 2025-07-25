package com.cheatkey.module.auth.domain.entity.token;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "t_auth_refresh_token")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
    @Id
    private Long userId;

    @Column(nullable = false, unique = true)
    private String token;

    public void updateToken(String newToken) {
        this.token = newToken;
    }
} 