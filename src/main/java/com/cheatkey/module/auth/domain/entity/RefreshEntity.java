package com.cheatkey.module.auth.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "refresh_entity", indexes = {
        @Index(name = "idx_kakao_id", columnList = "kakao_id")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "kakao_id", nullable = false, unique = true)
    private Long kakaoId;

    @Column(name = "refresh", nullable = false, length = 2048)
    private String refresh;

    @Column(name = "expiration", nullable = false)
    private String expiration;

    @Builder
    public RefreshEntity(Long kakaoId, String refresh, String expiration) {
        this.kakaoId = kakaoId;
        this.refresh = refresh;
        this.expiration = expiration;
    }
}
