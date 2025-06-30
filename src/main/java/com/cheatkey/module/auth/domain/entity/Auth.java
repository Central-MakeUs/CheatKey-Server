package com.cheatkey.module.auth.domain.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@Entity
@Table(name = "t_auth", uniqueConstraints = @UniqueConstraint(columnNames = "kakaoId"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Auth {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long kakaoId;
    private String nickname;
    private String ageCode;
    private String genderCode;
    private String tradeMethodCode;
    private String tradeItemCode;

    private Integer loginCount;

    private LocalDateTime lastLoginAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;


    public void assignKakaoId(Long kakaoId) {
        this.kakaoId = kakaoId;
    }

    public void increaseLoginCount() {
        this.loginCount = (this.loginCount == null) ? 1 : this.loginCount + 1;
    }

    public void updateLastLoginTime(LocalDateTime now) {
        this.lastLoginAt = now;
    }

    public List<String> getTradeMethodCodes() {
        return tradeMethodCode != null
                ? Arrays.asList(tradeMethodCode.split(","))
                : Collections.emptyList();
    }

    public List<String> getTradeItemCodes() {
        return tradeItemCode != null
                ? Arrays.asList(tradeItemCode.split(","))
                : Collections.emptyList();
    }

    public void setTradeMethodCodes(List<String> codes) {
        this.tradeMethodCode = String.join(",", codes);
    }

    public void setTradeItemCodes(List<String> codes) {
        this.tradeItemCode = String.join(",", codes);
    }
}
