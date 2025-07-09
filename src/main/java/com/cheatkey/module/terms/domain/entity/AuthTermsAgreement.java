package com.cheatkey.module.terms.domain.entity;

import com.cheatkey.module.auth.domain.entity.Auth;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "t_auth_terms_agreement")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthTermsAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_id")
    private Auth auth;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "terms_id")
    private Terms terms;

    private String version;

    private LocalDateTime agreedAt;
}

