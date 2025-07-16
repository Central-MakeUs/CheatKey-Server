package com.cheatkey.module.auth.domain.repository;

import com.cheatkey.module.auth.domain.entity.Auth;
import com.cheatkey.module.auth.domain.entity.Provider;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Long> {
    boolean existsByNickname(String nickname);

    Optional<Auth> findByProviderAndProviderId(Provider provider, String providerId);
}
