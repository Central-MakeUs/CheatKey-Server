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
@Table(name = "t_auth")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Auth {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Provider provider;
    private String providerId;      // 카카오ID, 애플ID 등

    private String email;
    private String nickname;
    private String ageCode;
    private String genderCode;
    private String tradeMethodCode;
    private String tradeItemCode;

    private Long profileImageId;    // 프로필 이미지 ID

    private Integer loginCount;
    private Integer level;          // 사용자 레벨
    private LocalDateTime lastLoginAt;
    
    // 방문 횟수 관련 필드
    private Integer totalVisitCount;
    private LocalDateTime lastVisitDate;

    @Enumerated(EnumType.STRING)
    private AuthStatus status;

    @Enumerated(EnumType.STRING)
    private AuthRole role;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void increaseLoginCount() {
        this.loginCount = (this.loginCount == null) ? 1 : this.loginCount + 1;
    }

    public void updateLastLoginTime(LocalDateTime now) {
        this.lastLoginAt = now;
    }

    public void updateLastVisitDate(LocalDateTime now) {
        this.lastVisitDate = now;
    }

    public void increaseVisitCount() {
        this.totalVisitCount = (this.totalVisitCount == null) ? 1 : this.totalVisitCount + 1;
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
    public void setStatus(AuthStatus status) {
        this.status = status;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
    public void setAgeCode(String ageCode) {
        this.ageCode = ageCode;
    }
    public void setGenderCode(String genderCode) {
        this.genderCode = genderCode;
    }
    public void setProfileImageId(Long profileImageId) {this.profileImageId = profileImageId;}
    public void setLevel(Integer level) {this.level = level;}
}
